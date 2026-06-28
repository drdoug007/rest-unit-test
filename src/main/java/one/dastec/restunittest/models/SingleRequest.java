package one.dastec.restunittest.models;

import java.util.Map;

public class SingleRequest {
    private String name;
    private String content;
    private String requestLine;
    private int lineIndex;
    private Map<String, Object> globals;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getRequestLine() { return requestLine; }
    public void setRequestLine(String requestLine) { this.requestLine = requestLine; }

    public int getLineIndex() { return lineIndex; }
    public void setLineIndex(int lineIndex) { this.lineIndex = lineIndex; }

    public Map<String, Object> getGlobals() { return globals; }
    public void setGlobals(Map<String, Object> globals) { this.globals = globals; }
}
