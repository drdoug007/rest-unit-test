package one.dastec.restunittest.debugger;

import com.oracle.truffle.api.debug.*;
import com.oracle.truffle.api.instrumentation.SourceFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DebuggerService {

    private static final Logger log = LoggerFactory.getLogger(DebuggerService.class);
    private DebuggerWebSocketHandler webSocketHandler;
    private final Map<Integer, Boolean> globalBreakpoints = new ConcurrentHashMap<>();
    private final Map<String, Integer> sourceOffsets = new ConcurrentHashMap<>();
    private final Map<String, java.net.URI> sourceURIs = new ConcurrentHashMap<>();
    private final Semaphore pauseLock = new Semaphore(0);
    private final AtomicBoolean isDebugging = new AtomicBoolean(false);
    private String steppingMode = "NONE"; // NONE, OVER, INTO, OUT
    private DebuggerSession debuggerSession;
    private final List<Breakpoint> installedBreakpoints = new CopyOnWriteArrayList<>();
    private String lastPausedSource = null;
    private int lastPausedLine = -1;
    private int lastPausedStackDepth = -1;

    public void setWebSocketHandler(DebuggerWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    public synchronized void setBreakpoints(String testName, Map<Integer, Boolean> testBreakpoints) {
        globalBreakpoints.clear();
        globalBreakpoints.putAll(testBreakpoints);
        log.info("Breakpoints updated for test {}: {}", testName, testBreakpoints);
        
        if (debuggerSession != null) {
            refreshBreakpoints();
        }
    }

    private synchronized void refreshBreakpoints() {
        log.info("Refreshing breakpoints. Current sources: {}", sourceOffsets.keySet());
        for (Breakpoint bp : installedBreakpoints) {
            bp.dispose();
        }
        installedBreakpoints.clear();

        for (Map.Entry<String, Integer> sourceEntry : sourceOffsets.entrySet()) {
            String sourceName = sourceEntry.getKey();
            int offset = sourceEntry.getValue();
            java.net.URI uri = sourceURIs.get(sourceName);
            if (uri != null) {
                installBreakpointsForSource(sourceName, offset, uri);
            }
        }
    }

    public synchronized void registerSourceOffset(String sourceName, java.net.URI uri, int offset) {
        log.info("Registering source offset: {} (URI: {}) = {}", sourceName, uri, offset);
        sourceOffsets.put(sourceName, offset);
        sourceURIs.put(sourceName, uri);
        if (debuggerSession != null) {
            installBreakpointsForSource(sourceName, offset, uri);
        }
    }

    private void installBreakpointsForSource(String sourceName, int offset, java.net.URI uri) {
        log.info("Installing breakpoints for source {} (URI: {}), globalBreakpoints: {}", sourceName, uri, globalBreakpoints);
        for (Map.Entry<Integer, Boolean> entry : globalBreakpoints.entrySet()) {
            if (entry.getValue()) {
                int absLine = entry.getKey();
                int relLine = absLine - offset + 1;
                if (relLine > 0) {
                    try {
                        log.info("Creating breakpoint at absolute line {}, relative line {}", absLine, relLine);
                        Breakpoint bp = Breakpoint.newBuilder(uri)
                                .lineIs(relLine)
                                .build();
                        debuggerSession.install(bp);
                        installedBreakpoints.add(bp);
                        log.info("Breakpoint INSTALLED successfully");
                    } catch (Exception e) {
                        log.error("Failed to install breakpoint for {} at rel line {}", sourceName, relLine, e);
                    }
                } else {
                    log.info("Skipping breakpoint at abs line {} (rel line {} <= 0)", absLine, relLine);
                }
            }
        }
    }

    public boolean isDebugging() {
        return isDebugging.get();
    }

    public void startDebugging() {
        isDebugging.set(true);
        steppingMode = "NONE";
        sourceOffsets.clear();
        sourceURIs.clear();
        pauseLock.drainPermits();
        lastPausedSource = null;
        lastPausedLine = -1;
        lastPausedStackDepth = -1;
    }

    public void stopDebugging() {
        isDebugging.set(false);
        steppingMode = "NONE";
        if (debuggerSession != null) {
            debuggerSession.close();
            debuggerSession = null;
        }
        installedBreakpoints.clear();
        pauseLock.release(100); // Release any waiting threads
        lastPausedSource = null;
        lastPausedLine = -1;
        lastPausedStackDepth = -1;
    }

    public void resume() {
        steppingMode = "NONE";
        lastPausedSource = null;
        lastPausedLine = -1;
        lastPausedStackDepth = -1;
        pauseLock.release();
    }

    public void stepOver() {
        steppingMode = "OVER";
        pauseLock.release();
    }

    public void stepInto() {
        steppingMode = "INTO";
        pauseLock.release();
    }

    public void stepOut() {
        steppingMode = "OUT";
        pauseLock.release();
    }

    public void stop() {
        stopDebugging();
    }

    public void attachToEngine(org.graalvm.polyglot.Engine engine) {
        if (!isDebugging.get()) return;

        if (debuggerSession != null) {
            debuggerSession.close();
        }

        Debugger debugger = engine.getInstruments().get("debugger").lookup(Debugger.class);
        debuggerSession = debugger.startSession(this::handleOnSuspend);
        
        // Trigger initial suspension if requested
        debuggerSession.suspendNextExecution();

        // Apply existing source offsets and breakpoints
        refreshBreakpoints();
    }

    private void handleOnSuspend(SuspendedEvent event) {
        String sourceName = event.getTopStackFrame().getSourceSection().getSource().getName();
        java.net.URI uri = event.getTopStackFrame().getSourceSection().getSource().getURI();
        log.info("Suspended at source {} (URI: {})", sourceName, uri);
        
        // Only pause for our registered sources
        if (!sourceOffsets.containsKey(sourceName)) {
            log.info("Ignoring suspension for unregistered source: {}", sourceName);
            applyStepping(event);
            return;
        }

        int relLine = event.getTopStackFrame().getSourceSection().getStartLine();
        int totalLines = event.getTopStackFrame().getSourceSection().getSource().getLineCount();
        
        // Skip synthesized IIFE lines (first and last)
        if (relLine <= 1 || relLine >= totalLines) {
            if (relLine >= totalLines && !steppingMode.equals("NONE")) {
                // Bridge the gap between multiple script blocks (eval calls)
                debuggerSession.suspendNextExecution();
            }
            applyStepping(event);
            return;
        }

        int offset = sourceOffsets.getOrDefault(sourceName, 0);
        int absLine = relLine + offset - 1;
        int currentStackDepth = 0;
        for (DebugStackFrame frame : event.getStackFrames()) {
            currentStackDepth++;
        }

        // Skip redundant pauses at the same line (e.g. when returning from a function to the call site)
        if (sourceName.equals(lastPausedSource) && absLine == lastPausedLine) {
            log.info("Already paused at {}:{}, skipping redundant pause", sourceName, absLine);
            applyStepping(event);
            return;
        }
        
        // Skip redundant pause at call site when returning from a function
        if (currentStackDepth < lastPausedStackDepth) {
            log.info("Returning from function to {}:{}, skipping pause at call site", sourceName, absLine);
            lastPausedStackDepth = currentStackDepth;
            applyStepping(event);
            return;
        }

        Map<String, Object> variables = collectVariables(event);
        lastPausedSource = sourceName;
        lastPausedLine = absLine;
        lastPausedStackDepth = currentStackDepth;
        
        // Reset stepping mode BEFORE pausing so that subsequent stepping commands can set it
        steppingMode = "NONE";
        pauseExecution(sourceName, absLine, variables);

        // After unblocking (user clicked Resume/Step), apply stepping if needed
        applyStepping(event);
    }

    private void applyStepping(SuspendedEvent event) {
        String currentMode = steppingMode;
        int relLine = event.getTopStackFrame().getSourceSection().getStartLine();

        // If we are at the start of a script block (IIFE wrapper), we must step INTO
        // to enter the function, otherwise Step Over will skip the whole block.
        if (relLine <= 1 && !currentMode.equals("NONE")) {
            event.prepareStepInto(1);
            return;
        }

        if (currentMode.equals("OVER")) {
            event.prepareStepOver(1);
        } else if (currentMode.equals("INTO")) {
            event.prepareStepInto(1);
        } else if (currentMode.equals("OUT")) {
            event.prepareStepOut(1);
        } else {
            event.prepareContinue();
        }
    }

    private Map<String, Object> collectVariables(SuspendedEvent event) {
        Map<String, Object> variables = new LinkedHashMap<>();
        try {
            DebugStackFrame frame = event.getTopStackFrame();
            if (frame != null) {
                DebugScope scope = frame.getScope();
                while (scope != null) {
                    log.info("Collecting variables from scope: {}", scope.getName());
                    for (DebugValue value : scope.getDeclaredValues()) {
                        String name = value.getName();
                        if (!variables.containsKey(name)) {
                            variables.put(name, formatDebugValue(value, 0));
                        }
                    }
                    scope = scope.getParent();
                }
            }
            
            // Also try to get global variables from the top scope of the language
            try {
                DebugScope globalScope = event.getSession().getTopScope("js");
                if (globalScope != null) {
                    log.info("Collecting variables from global JS scope");
                    for (DebugValue value : globalScope.getDeclaredValues()) {
                        String name = value.getName();
                        if (!variables.containsKey(name)) {
                            variables.put(name, formatDebugValue(value, 0));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not access global JS scope: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error collecting variables", e);
        }
        return variables;
    }

    private String formatDebugValue(DebugValue value, int depth) {
        if (value == null) return "null";
        if (depth > 2) return "...";
        try {
            if (value.isNull()) return "null";
            if (value.isString()) return value.as(String.class); // No quotes to match existing tests
            if (value.isBoolean()) return String.valueOf(value.asBoolean());
            if (value.isNumber()) return value.toDisplayString(false);

            // Try as host object for Maps and Lists directly
            try {
                Map<?, ?> map = value.as(Map.class);
                if (map != null) return formatHostObject(map, depth);
            } catch (Exception e) { /* Not a map */ }

            try {
                List<?> list = value.as(List.class);
                if (list != null) return formatHostObject(list, depth);
            } catch (Exception e) { /* Not a list */ }

            if (value.isArray()) {
                List<DebugValue> elements = value.getArray();
                int size = elements.size();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < Math.min(size, 10); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(formatDebugValue(elements.get(i), depth + 1));
                }
                if (size > 10) sb.append(", ... (" + size + " total)");
                sb.append("]");
                return sb.toString();
            }

            if (value.hasHashEntries()) {
                long size = value.getHashSize();
                StringBuilder sb = new StringBuilder("{");
                DebugValue iterator = value.getHashEntriesIterator();
                int count = 0;
                while (iterator.hasIteratorNextElement() && count < 10) {
                    if (count > 0) sb.append(", ");
                    DebugValue entry = iterator.getIteratorNextElement();
                    List<DebugValue> pair = entry.getArray();
                    if (pair.size() >= 2) {
                        sb.append(formatDebugValue(pair.get(0), depth + 1))
                          .append(": ")
                          .append(formatDebugValue(pair.get(1), depth + 1));
                    }
                    count++;
                }
                if (iterator.hasIteratorNextElement()) sb.append(", ...");
                sb.append("}");
                return sb.toString();
            }

            // Check if it's an object with properties
            java.util.Collection<DebugValue> properties = value.getProperties();
            if (properties != null && !properties.isEmpty()) {
                StringBuilder sb = new StringBuilder("{");
                int count = 0;
                for (DebugValue prop : properties) {
                    String propName = prop.getName();
                    // Heuristic: skip common Java methods that might show up as properties
                    if (propName.equals("getClass") || propName.equals("toString") || propName.equals("hashCode") || 
                        propName.equals("wait") || propName.equals("notify") || propName.equals("notifyAll") ||
                        propName.startsWith("is") || propName.startsWith("get") || propName.startsWith("set")) {
                        // For Maps/Lists we already handled them, for others this might be too aggressive
                        // but usually we want data, not methods.
                        // Let's be a bit more selective.
                        if (propName.length() > 2 && Character.isUpperCase(propName.charAt(2))) {
                             // likely getter, skip
                             continue;
                        }
                    }

                    if (count > 0) sb.append(", ");
                    if (count >= 10) {
                        sb.append("...");
                        break;
                    }
                    sb.append(propName).append(": ").append(formatDebugValue(prop, depth + 1));
                    count++;
                }
                sb.append("}");
                if (count > 0) return sb.toString();
            }

            return value.toDisplayString(false);
        } catch (Exception e) {
            return "(Error: " + e.getClass().getSimpleName() + ")";
        }
    }

    private String formatHostObject(Object obj, int depth) {
        if (obj == null) return "null";
        if (depth > 3) return "...";
        if (obj instanceof String) return (String) obj;
        if (obj instanceof Number || obj instanceof Boolean) return String.valueOf(obj);
        
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            int count = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (count > 0) sb.append(", ");
                if (count >= 10) {
                    sb.append("...");
                    break;
                }
                sb.append(entry.getKey()).append(": ").append(formatHostObject(entry.getValue(), depth + 1));
                count++;
            }
            sb.append("}");
            return sb.toString();
        }
        
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < Math.min(list.size(), 10); i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatHostObject(list.get(i), depth + 1));
            }
            if (list.size() > 10) sb.append(", ... (" + list.size() + " total)");
            sb.append("]");
            return sb.toString();
        }
        
        return String.valueOf(obj);
    }

    private void pauseExecution(String sourceName, int absoluteLine, Map<String, Object> variables) {
        log.info("Execution paused at {} line {}", sourceName, absoluteLine);
        if (webSocketHandler != null) {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "paused");
            message.put("sourceName", sourceName);
            message.put("line", absoluteLine);
            message.put("variables", variables);
            webSocketHandler.sendMessage(message);
        }

        try {
            pauseLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (webSocketHandler != null) {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "resumed");
            webSocketHandler.sendMessage(message);
        }
    }
}
