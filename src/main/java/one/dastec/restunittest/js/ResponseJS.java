package one.dastec.restunittest.js;

import java.util.Map;

public class ResponseJS {
    public final int status;
    public final Map<String, String> headers;
    public Object body;
    public String bodyRaw;
    public final ContentType contentType;

    public ResponseJS(int status, Map<String, String> headers, Object body, String bodyRaw) {
        this.status = status;
        this.headers = headers;
        this.body = body;
        this.bodyRaw = bodyRaw;
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
        public final String charset;

        public ContentType(String contentTypeHeader) {
            if (contentTypeHeader != null) {
                // Handle cases like "application/json; charset=utf-8"
                String[] parts = contentTypeHeader.split(";");
                this.mimeType = parts[0].trim();
                String cs = null;
                for (int i = 1; i < parts.length; i++) {
                    String part = parts[i].trim();
                    if (part.toLowerCase().startsWith("charset=")) {
                        cs = part.substring(8).trim();
                        break;
                    }
                }
                this.charset = cs;
            } else {
                this.mimeType = null;
                this.charset = null;
            }
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getCharset() {
            return charset;
        }

        public boolean contains(String text) {
            return mimeType != null && mimeType.contains(text);
        }

        public boolean includes(String text) {
            return contains(text);
        }

        public boolean equals(Object obj) {
            if (obj instanceof String) {
                return obj.equals(mimeType);
            }
            return super.equals(obj);
        }

        @Override
        public String toString() {
            return mimeType != null ? mimeType : "";
        }
    }

    public int getStatus() {
        return status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Object getBody() {
        return body;
    }

    public String getBodyRaw() {
        return bodyRaw;
    }
}
