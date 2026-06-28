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
    private Integer timeout;
    private Integer connectionTimeout;
    private List<ScriptAction> preActions = new ArrayList<>();
    private List<ScriptAction> postActions = new ArrayList<>();
    private MockResponse mockResponse;
    private int postScriptLineOffset;

    public static class MockResponse {
        private String status;
        private Map<String, String> headers = new HashMap<>();
        private String body;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }

    public static class ScriptAction {
        private final String type; // JS or SQL
        private final String content;
        private final int lineOffset;

        public ScriptAction(String type, String content) {
            this(type, content, 0);
        }

        public ScriptAction(String type, String content, int lineOffset) {
            this.type = type;
            this.content = content;
            this.lineOffset = lineOffset;
        }

        public String getType() { return type; }
        public String getContent() { return content; }
        public int getLineOffset() { return lineOffset; }
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
    public Integer getTimeout() { return timeout; }
    public void setTimeout(Integer timeout) { this.timeout = timeout; }
    public Integer getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(Integer connectionTimeout) { this.connectionTimeout = connectionTimeout; }
    public List<ScriptAction> getPreActions() { return preActions; }
    public void setPreActions(List<ScriptAction> preActions) { this.preActions = preActions; }
    public List<ScriptAction> getPostActions() { return postActions; }
    public void setPostActions(List<ScriptAction> postActions) { this.postActions = postActions; }
    public MockResponse getMockResponse() { return mockResponse; }
    public void setMockResponse(MockResponse mockResponse) { this.mockResponse = mockResponse; }
    public int getPostScriptLineOffset() { return postScriptLineOffset; }
    public void setPostScriptLineOffset(int postScriptLineOffset) { this.postScriptLineOffset = postScriptLineOffset; }
}
