package one.dastec.restunittest.js;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestJS {
    public final Variables variables = new Variables();
    private int iteration = 0;
    private List<Object> templateValues = new ArrayList<>();

    public Variables getVariables() {
        return variables;
    }

    public int getIteration() {
        return iteration;
    }

    public int iteration() {
        return iteration;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public Object templateValue(int index) {
        if (index >= 0 && index < templateValues.size()) {
            return templateValues.get(index);
        }
        return null;
    }

    public void setTemplateValues(List<Object> templateValues) {
        this.templateValues = templateValues;
    }

    public static class Variables {
        private final Map<String, Object> map = new HashMap<>();

        public void set(String name, Object value) {
            map.put(name, value);
        }

        public Object get(String name) {
            return map.get(name);
        }

        public void remove(String name) {
            map.remove(name);
        }

        public Map<String, Object> all() {
            return map;
        }
    }
}
