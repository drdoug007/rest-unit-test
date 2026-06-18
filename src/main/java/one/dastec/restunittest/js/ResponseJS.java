package one.dastec.restunittest.js;

import java.util.Map;

public class ResponseJS {
    public final int status;
    public final Map<String, String> headers;
    public final String body;
    public final ContentType contentType;

    public ResponseJS(int status, Map<String, String> headers, String body) {
        this.status = status;
        this.headers = headers;
        this.body = body;
        String ct = null;
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey() != null && "content-type".equalsIgnoreCase(entry.getKey())) {
                    ct = entry.getValue();
                    break;
                }
            }
        }
        this.contentType = new ContentType(ct);
    }

    public static class ContentType {
        public final String mimeType;

        public ContentType(String contentTypeHeader) {
            if (contentTypeHeader != null) {
                // Handle cases like "application/json; charset=utf-8"
                this.mimeType = contentTypeHeader.split(";")[0].trim();
            } else {
                this.mimeType = null;
            }
        }

        public String getMimeType() {
            return mimeType;
        }
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
