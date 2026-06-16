package one.dastec.restunittest.js;

import java.util.HashMap;
import java.util.Map;

public class RequestJS {
    public final Variables variables = new Variables();

    public Variables getVariables() {
        return variables;
    }

    public static class Variables {
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
