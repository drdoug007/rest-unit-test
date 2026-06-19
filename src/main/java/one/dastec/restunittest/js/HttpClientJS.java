package one.dastec.restunittest.js;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class HttpClientJS {
    public final Global global = new Global();
    private final List<String> testResults = new ArrayList<>();
    private final List<String> logs = new ArrayList<>();
    private final List<String> markdownEntries = new ArrayList<>();

    public void test(String name, Runnable callback) {
        try {
            callback.run();
            testResults.add("✅ " + name);
        } catch (Throwable t) {
            String message = t.getMessage();
            if (message != null && message.startsWith("Error: ")) {
                message = message.substring(7);
            }
            testResults.add("❌ " + name + ": " + message);
        }
    }

    public void log(String message) {
        logs.add(message);
    }

    public void markdown(String content) {
        markdownEntries.add(content);
    }

    public void assertCondition(Object condition, String message) {
        boolean boolCondition = false;
        if (condition instanceof Boolean) {
            boolCondition = (Boolean) condition;
        } else if (condition != null) {
            // Truthy check for non-null objects if they are not Boolean
            boolCondition = true;
        }
        
        if (!boolCondition) {
            throw new RuntimeException(message);
        }
    }

    public Global getGlobal() {
        return global;
    }

    public List<String> getTestResults() {
        return testResults;
    }

    public List<String> getLogs() {
        return logs;
    }

    public List<String> getMarkdownEntries() {
        return markdownEntries;
    }

    public static class Global {
        private final Map<String, Object> map = new HashMap<>();

        public void set(String name, Object value) {
            map.put(name, value);
        }

        public Object get(String name) {
            return map.get(name);
        }

        public Map<String, Object> all() {
            return map;
        }
    }
}
