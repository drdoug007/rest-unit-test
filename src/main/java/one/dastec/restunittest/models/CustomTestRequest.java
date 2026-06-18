package one.dastec.restunittest.models;

import java.util.Map;

public class CustomTestRequest {
    private String name;
    private String content;
    private Map<String, Object> globals;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getGlobals() {
        return globals;
    }

    public void setGlobals(Map<String, Object> globals) {
        this.globals = globals;
    }
}
