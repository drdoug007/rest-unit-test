package one.dastec.restunittest.js;

import org.graalvm.polyglot.Value;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class UrlSearchParamsJS {

    private final List<Map.Entry<String, String>> params = new ArrayList<>();

    public UrlSearchParamsJS(Object init) {
        if (init == null) return;

        if (init instanceof String) {
            parseQueryString((String) init);
        } else if (init instanceof Value) {
            Value v = (Value) init;
            if (v.hasArrayElements()) {
                for (long i = 0; i < v.getArraySize(); i++) {
                    Value pair = v.getArrayElement(i);
                    if (pair.hasArrayElements() && pair.getArraySize() >= 2) {
                        params.add(new AbstractMap.SimpleEntry<>(
                                pair.getArrayElement(0).toString(),
                                pair.getArrayElement(1).toString()
                        ));
                    }
                }
            } else if (v.hasMembers()) {
                for (String key : v.getMemberKeys()) {
                    params.add(new AbstractMap.SimpleEntry<>(key, v.getMember(key).toString()));
                }
            }
        } else {
            // Fallback for other types if they somehow get here as non-Value
            parseQueryString(init.toString());
        }
    }

    private void parseQueryString(String query) {
        if (query.startsWith("?")) {
            query = query.substring(1);
        }
        if (query.isEmpty()) return;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                if (idx > 0) {
                    params.add(new AbstractMap.SimpleEntry<>(
                            URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                            URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8)
                    ));
                } else if (!pair.isEmpty()) {
                    params.add(new AbstractMap.SimpleEntry<>(
                            URLDecoder.decode(pair, StandardCharsets.UTF_8),
                            ""
                    ));
                }
            } catch (Exception e) {
                // Ignore decoding errors
            }
        }
    }

    public void append(String name, String value) {
        params.add(new AbstractMap.SimpleEntry<>(name, value));
    }

    public void delete(String name) {
        params.removeIf(e -> e.getKey().equals(name));
    }

    public String get(String name) {
        return params.stream()
                .filter(e -> e.getKey().equals(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public List<String> getAll(String name) {
        List<String> result = params.stream()
                .filter(e -> e.getKey().equals(name))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        return result;
    }

    public boolean has(String name) {
        return params.stream().anyMatch(e -> e.getKey().equals(name));
    }

    public void set(String name, String value) {
        boolean found = false;
        Iterator<Map.Entry<String, String>> it = params.iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if (e.getKey().equals(name)) {
                if (!found) {
                    e.setValue(value);
                    found = true;
                } else {
                    it.remove();
                }
            }
        }
        if (!found) {
            params.add(new AbstractMap.SimpleEntry<>(name, value));
        }
    }

    public void sort() {
        params.sort(Map.Entry.comparingByKey());
    }

    public List<String> keys() {
        return params.stream().map(Map.Entry::getKey).collect(Collectors.toList());
    }

    public List<String> values() {
        return params.stream().map(Map.Entry::getValue).collect(Collectors.toList());
    }

    public List<List<String>> entries() {
        return params.stream()
                .map(e -> Arrays.asList(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return params.stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        if (value == null) return "";
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }
}
