package one.dastec.restunittest.js;

import java.util.Map;

public class ResponseJS {
    public final int status;
    public final Map<String, String> headers;
    public final String body;

    public ResponseJS(int status, Map<String, String> headers, String body) {
        this.status = status;
        this.headers = headers;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}
