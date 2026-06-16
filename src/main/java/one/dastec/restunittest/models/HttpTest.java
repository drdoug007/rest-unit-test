package one.dastec.restunittest.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class HttpTest {
    private String name;
    private String preScript;
    private String method;
    private String url;
    private Map<String, String> headers = new HashMap<>();
    private String body;
    private String sqlScript;
    private String postScript;
    private List<PreAction> preActions = new ArrayList<>();

    public static class PreAction {
        private final String type; // JS or SQL
        private final String content;

        public PreAction(String type, String content) {
            this.type = type;
            this.content = content;
        }

        public String getType() { return type; }
        public String getContent() { return content; }
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPreScript() { return preScript; }
    public void setPreScript(String preScript) { this.preScript = preScript; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getSqlScript() { return sqlScript; }
    public void setSqlScript(String sqlScript) { this.sqlScript = sqlScript; }
    public String getPostScript() { return postScript; }
    public void setPostScript(String postScript) { this.postScript = postScript; }
    public List<PreAction> getPreActions() { return preActions; }
    public void setPreActions(List<PreAction> preActions) { this.preActions = preActions; }
}
