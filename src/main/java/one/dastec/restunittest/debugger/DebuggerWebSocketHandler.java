package one.dastec.restunittest.debugger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DebuggerWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DebuggerWebSocketHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final DebuggerService debuggerService;

    public DebuggerWebSocketHandler(DebuggerService debuggerService) {
        this.debuggerService = debuggerService;
        this.debuggerService.setWebSocketHandler(this);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("Debugger WebSocket connection established: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("Debugger WebSocket connection closed: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
        String type = (String) payload.get("type");
        log.info("Received debugger command: {}", type);

        switch (type) {
            case "setBreakpoints" -> {
                String testName = (String) payload.get("testName");
                Map<String, Boolean> rawBreakpoints = (Map<String, Boolean>) payload.get("breakpoints");
                Map<Integer, Boolean> intBreakpoints = new java.util.HashMap<>();
                if (rawBreakpoints != null) {
                    rawBreakpoints.forEach((k, v) -> intBreakpoints.put(Integer.parseInt(k), v));
                }
                debuggerService.setBreakpoints(testName, intBreakpoints);
            }
            case "resume" -> debuggerService.resume();
            case "stepOver" -> debuggerService.stepOver();
            case "stepInto" -> debuggerService.stepInto();
            case "stepOut" -> debuggerService.stepOut();
            case "stop" -> debuggerService.stop();
        }
    }

    public void sendMessage(Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        } catch (IOException e) {
            log.error("Error sending debugger message", e);
        }
    }
}
