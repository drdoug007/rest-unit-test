package one.dastec.restunittest.services;

import one.dastec.restunittest.config.AppProperties;
import one.dastec.restunittest.js.*;
import one.dastec.restunittest.models.HttpTest;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RestTestService {

    private static final Logger log = LoggerFactory.getLogger(RestTestService.class);
    private final JdbcTemplate jdbcTemplate;
    private final RestClient.Builder builder;
    private final GraalJsService graalJsService;
    private final AppProperties appProperties;


    private final UtilsJS utilsJS = new UtilsJS();
    private final CryptoJS cryptoJS = new CryptoJS();
    private final SubtleCryptoJS subtleCryptoJS = new SubtleCryptoJS();
    private final JwtJS jwtJS = new JwtJS();

    public RestTestService(DataSource dataSource, JdbcTemplate jdbcTemplate, RestClient.Builder builder, GraalJsService graalJsService, AppProperties appProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.builder = builder;
        this.graalJsService = graalJsService;
        this.appProperties = appProperties;
    }

    public String getTestSource(String testName) {
        var testPath = "httptestfiles/" + testName + ".http";
        Resource testFile;
        if (testPath.startsWith("/") || (testPath.length() > 1 && testPath.charAt(1) == ':')) {
            testFile = new FileSystemResource(testPath);
        } else {
            testFile = new ClassPathResource(testPath);
        }
        if (!testFile.exists()) {
            log.error("Test not found: {}", testPath);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test not found: " + testName);
        }
        try {
            return testFile.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error reading test file: " + testPath, e);
        }
    }

    public String runTest(String testName) {
        var content = getTestSource(testName);
        return runTestWithContent(testName, content);
    }

    public String fetchExternalUrl(String url) {
        try {
            return builder.build().get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Error fetching external URL {}: {}", url, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error fetching URL: " + e.getMessage());
        }
    }

    public String runTestWithContent(String testName, String content) {
        return runTestWithContent(testName, content, null);
    }

    public String runTestWithContent(String testName, String content, Map<String, Object> globals) {
        try (org.graalvm.polyglot.Context context = graalJsService.createContext()) {
            // ... existing code ...
            Map<String, String> inplaceVariables = parseInplaceVariables(content);
            List<HttpTest> tests = parseHttpFile(content);
            StringBuilder report = new StringBuilder();
            if (appProperties != null && appProperties.getEnvironment() != null) {
                report.append("Environment: ").append(appProperties.getEnvironment().getName()).append("\n\n");
            }
            report.append("# Test Report: ").append(testName).append("\n\n");

            RequestJS requestJS = new RequestJS();
            HttpClientJS httpClientJS = new HttpClientJS();

            // Expose app properties to JS and populate variables
            if (appProperties != null) {
                if (appProperties.getEnvironment() != null) {
                    context.getBindings("js").putMember("environment", appProperties.getEnvironment());
                    String baseUrl = appProperties.getEnvironment().getBaseUrl();
                    if (baseUrl != null) {
                        requestJS.getVariables().set("baseUrl", baseUrl);
                    } else {
                        requestJS.getVariables().set("baseUrl", "{{baseUrl}}");
                    }
                    if (appProperties.getEnvironment().getName() != null) {
                        requestJS.getVariables().set("environmentName", appProperties.getEnvironment().getName());
                    }
                }
                
                // Load global variables from app properties
                if (appProperties.getTestGlobals() != null) {
                    appProperties.getTestGlobals().forEach((k, v) -> {
                        if (v != null) {
                            requestJS.getVariables().set(k, v);
                            httpClientJS.getGlobal().set(k, v);
                        }
                    });
                }
                
                // Also expose the whole app properties if needed
                context.getBindings("js").putMember("app", appProperties);
            }

            // Load incoming globals if provided
            if (globals != null) {
                globals.forEach((k, v) -> {
                    if (v != null) {
                        requestJS.getVariables().set(k, v);
                        httpClientJS.getGlobal().set(k, v);
                    }
                });
            }

            // Load in-place variables from the file content
            inplaceVariables.forEach((k, v) -> {
                requestJS.getVariables().set(k, v);
                httpClientJS.getGlobal().set(k, v);
            });

        // Load markdown.js helper into GraalJS context
        try {
            ClassPathResource markdownResource = new ClassPathResource("js/markdown.js");
            if (markdownResource.exists()) {
                String markdownJs = markdownResource.getContentAsString(StandardCharsets.UTF_8);
                // Strip exports for non-module GraalJS eval
                markdownJs = markdownJs.replaceAll("(?m)^export ", "");
                context.eval("js", markdownJs);
            }
        } catch (IOException e) {
            log.warn("Could not load markdown.js: {}", e.getMessage());
        }

            for (HttpTest test : tests) {
                if (test.getMethod() == null || test.getUrl() == null) {
                    log.warn("Skipping test block with no request: {}", test.getName());
                    continue;
                }
                report.append("## ").append(test.getName()).append("\n\n");
                executeTest(test, requestJS, httpClientJS, report, context);
                report.append("\n---\n\n");
                
                // Propagate global variables back to requestJS for the next test in the same session
                httpClientJS.getGlobal().all().forEach((k, v) -> {
                    if (v != null) {
                        requestJS.getVariables().set(k, v);
                    }
                });
            }

            return report.toString();
        } catch (Exception e) {
            log.error("Error running test {}: {}", testName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> parseInplaceVariables(String content) {
        Map<String, String> variables = new HashMap<>();
        Pattern pattern = Pattern.compile("(?m)^@([^=\\s]+)\\s*=\\s*(.*)$");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            variables.put(key, value);
        }
        return variables;
    }

    private List<HttpTest> parseHttpFile(String content) {
        List<HttpTest> tests = new ArrayList<>();
        // Split by ### at the beginning of a line to avoid splitting on internal ###
        String[] blocks = content.split("(?m)^###");
        for (String block : blocks) {
            if (block.trim().isEmpty()) continue;
            tests.add(parseBlock("###" + block));
        }
        return tests;
    }

    private Integer parseTimeout(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            String numericPart = value.replaceAll("[^0-9]", "").trim();
            String unitPart = value.replaceAll("[0-9]", "").trim().toLowerCase();
            
            if (numericPart.isEmpty()) return null;
            int numericValue = Integer.parseInt(numericPart);
            
            if (unitPart.equals("ms")) {
                return numericValue;
            } else if (unitPart.equals("m")) {
                return numericValue * 60 * 1000;
            } else {
                // Default to seconds
                return numericValue * 1000;
            }
        } catch (Exception e) {
            log.warn("Could not parse timeout value: {}", value);
            return null;
        }
    }

    private boolean isRequestLine(String line) {
        String trimmedLine = line.trim();
        if (trimmedLine.isEmpty()) return false;
        String[] parts = trimmedLine.split("\\s+");
        if (parts.length >= 2) {
            String method = parts[0];
            String url = parts[1];
            if (method.matches("^(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)$")) {
                // To be a valid request line, the second part should look like a URL or a variable
                return url.startsWith("http") || url.startsWith("{{") || url.contains("/");
            }
        }
        return false;
    }

    private HttpTest parseBlock(String block) {
        HttpTest test = new HttpTest();
        String[] lines = block.split("\\r?\\n");
        
        int firstNonEmptyLine = -1;
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].trim().isEmpty()) {
                firstNonEmptyLine = i;
                break;
            }
        }
        
        if (firstNonEmptyLine == -1) return test;

        // Parse timeouts from comments before the request
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("#") || line.startsWith("//")) {
                String comment = line.substring(line.startsWith("#") ? 1 : 2).trim();
                if (comment.startsWith("@timeout")) {
                    test.setTimeout(parseTimeout(comment.substring(8).trim()));
                } else if (comment.startsWith("@connection-timeout")) {
                    test.setConnectionTimeout(parseTimeout(comment.substring(19).trim()));
                }
            }
        }

        String firstLine = lines[firstNonEmptyLine].trim();
        String cleanFirstLine = firstLine.replaceAll("^###", "").trim();
        log.info("Parsing block starting with: {}", cleanFirstLine);
        
        // Check if first line is a request line (starts with HTTP method and has a URL)
        boolean firstLineIsRequest = false;
        String[] firstLineParts = cleanFirstLine.split("\\s+");
        if (firstLineParts.length >= 2 && firstLineParts.length <= 3) {
            firstLineIsRequest = firstLineParts[0].matches("^(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)$");
        }
        
        test.setName(cleanFirstLine);

        StringBuilder preScript = new StringBuilder();
        StringBuilder postScript = new StringBuilder();
        StringBuilder body = new StringBuilder();
        Map<String, String> headers = new HashMap<>();
        String method = null;
        String url = null;
        
        // Scan for request line if not the first line
        for (int i = firstNonEmptyLine; i < lines.length; i++) {
            String l = lines[i].trim();
            if (l.isEmpty()) continue;
            if (l.startsWith("< {%") || l.startsWith("> {%") || l.startsWith("#") || l.startsWith("//") || l.startsWith("%}")) continue;
            
            if (isRequestLine(l)) {
                String[] parts = l.split("\\s+");
                method = parts[0];
                url = parts[1];
                break;
            }
        }

        String mode = "NONE"; // NONE, PRE, POST, SQL, REQUEST_BODY

        int startParsingFrom = firstNonEmptyLine;
        StringBuilder currentScript = new StringBuilder();
        boolean requestLineFound = false;

        for (int i = startParsingFrom; i < lines.length; i++) {
            String line = lines[i];
            if (i == firstNonEmptyLine) {
                line = cleanFirstLine;
            }
            String trimmedLine = line.trim();

            if (trimmedLine.startsWith("< {%")) {
                if (!currentScript.toString().trim().isEmpty()) {
                    if (mode.equals("PRE")) test.getPreActions().add(new HttpTest.PreAction("JS", currentScript.toString().trim()));
                    if (mode.equals("SQL")) test.getPreActions().add(new HttpTest.PreAction("SQL", currentScript.toString().trim()));
                    if (mode.equals("POST")) postScript.append(currentScript);
                }
                currentScript = new StringBuilder();
                mode = "PRE";
                continue;
            } else if (trimmedLine.startsWith("> {%SQL") || trimmedLine.startsWith("# < SQL")) {
                if (!currentScript.toString().trim().isEmpty()) {
                    if (mode.equals("PRE")) test.getPreActions().add(new HttpTest.PreAction("JS", currentScript.toString().trim()));
                    if (mode.equals("SQL")) test.getPreActions().add(new HttpTest.PreAction("SQL", currentScript.toString().trim()));
                    if (mode.equals("POST")) postScript.append(currentScript);
                }
                currentScript = new StringBuilder();
                mode = "SQL";
                continue;
            } else if (trimmedLine.startsWith("> {%")) {
                if (!currentScript.toString().trim().isEmpty()) {
                    if (mode.equals("PRE")) test.getPreActions().add(new HttpTest.PreAction("JS", currentScript.toString().trim()));
                    if (mode.equals("SQL")) test.getPreActions().add(new HttpTest.PreAction("SQL", currentScript.toString().trim()));
                    if (mode.equals("POST")) postScript.append(currentScript);
                }
                currentScript = new StringBuilder();
                mode = "POST";
                continue;
            } else if (trimmedLine.startsWith("%}") || trimmedLine.equals("# SQL")) {
                if (!currentScript.toString().trim().isEmpty()) {
                    if (mode.equals("PRE")) test.getPreActions().add(new HttpTest.PreAction("JS", currentScript.toString().trim()));
                    if (mode.equals("SQL")) test.getPreActions().add(new HttpTest.PreAction("SQL", currentScript.toString().trim()));
                    if (mode.equals("POST")) postScript.append(currentScript);
                }
                currentScript = new StringBuilder();
                mode = "NONE";
                continue;
            }

            if (mode.equals("PRE") || mode.equals("SQL") || mode.equals("POST")) {
                currentScript.append(line).append("\n");
            } else if (mode.equals("NONE")) {
                if (trimmedLine.isEmpty()) {
                    if (requestLineFound) {
                        mode = "REQUEST_BODY";
                    }
                    continue;
                }
                if (trimmedLine.startsWith("@")) {
                    continue;
                }
                if (!requestLineFound) {
                    if (isRequestLine(trimmedLine)) {
                        String[] parts = trimmedLine.split("\\s+");
                        method = parts[0];
                        url = parts[1];
                        requestLineFound = true;
                    }
                } else if (trimmedLine.contains(":") && !trimmedLine.startsWith("//") && !trimmedLine.startsWith("/*")) {
                    int colonIndex = trimmedLine.indexOf(":");
                    String headerName = trimmedLine.substring(0, colonIndex).trim();
                    // Basic validation for header name (no spaces, etc.)
                    if (!headerName.contains(" ") && !headerName.isEmpty()) {
                        headers.put(headerName, trimmedLine.substring(colonIndex + 1).trim());
                    }
                }
            } else if (mode.equals("REQUEST_BODY")) {
                if (trimmedLine.startsWith("@")) {
                    continue;
                }
                body.append(line).append("\n");
            }
        }

        // Handle any remaining script in currentScript if loop finishes without %}
        if (!currentScript.toString().trim().isEmpty()) {
            if (mode.equals("PRE")) test.getPreActions().add(new HttpTest.PreAction("JS", currentScript.toString().trim()));
            if (mode.equals("SQL")) test.getPreActions().add(new HttpTest.PreAction("SQL", currentScript.toString().trim()));
            if (mode.equals("POST")) postScript.append(currentScript);
        }

        test.setPreScript(preScript.toString().trim());
        test.setPostScript(postScript.toString().trim());
        test.setMethod(method);
        test.setUrl(url);
        test.setHeaders(headers);
        test.setBody(body.toString().trim());

        return test;
    }

    private void executeTest(HttpTest test, RequestJS requestJS, HttpClientJS httpClientJS, StringBuilder report, org.graalvm.polyglot.Context context) {
        try {
            // 1. Pre-actions (JS and SQL in order)
            for (HttpTest.PreAction action : test.getPreActions()) {
                if ("SQL".equals(action.getType())) {
                    executeSql(action.getContent(), requestJS, httpClientJS, report);
                } else if ("JS".equals(action.getType())) {
                    setupGraalJsContext(requestJS, httpClientJS, null, context);
                    try {
                        String wrappedScript = "(function() { " + action.getContent() + " \n})();";
                        context.eval("js", wrappedScript);
                    } catch (Exception e) {
                        report.append("❌ **Error in pre-script:** ").append(e.getMessage()).append("\n");
                    }
                    appendResults(httpClientJS, report);
                    
                    // Sync global variables set in pre-script back to requestJS
                    httpClientJS.getGlobal().all().forEach((k, v) -> {
                        if (v != null) {
                            requestJS.getVariables().set(k, v);
                        }
                    });
                }
            }

            // Sync global variables to requestJS before resolving variables for the request
            httpClientJS.getGlobal().all().forEach((k, v) -> {
                if (v != null) {
                    requestJS.getVariables().set(k, v);
                }
            });

            // 2. Resolve variables
            Map<String, Object> allVars = new HashMap<>(requestJS.getVariables().all());
            httpClientJS.getGlobal().all().forEach((k, v) -> {
                if (v != null) {
                    allVars.put(k, v);
                }
            });

            // Identify if any variable used in the request is a collection
            String urlTemplate = test.getUrl();
            String bodyTemplate = test.getBody();
            Map<String, String> headerTemplates = test.getHeaders();
            
            String firstCollectionVar = null;
            String firstCollectionPath = null;
            List<Object> collectionValues = null;

            // Pattern for {{varName}}
            Pattern varPattern = Pattern.compile("\\{\\{(.+?)}}");
            
            // Collect all used variable names
            Set<String> usedVars = new HashSet<>();
            if (urlTemplate != null) {
                Matcher m = varPattern.matcher(urlTemplate);
                while (m.find()) usedVars.add(m.group(1).trim());
            }
            if (bodyTemplate != null) {
                Matcher m = varPattern.matcher(bodyTemplate);
                while (m.find()) usedVars.add(m.group(1).trim());
            }
            headerTemplates.values().forEach(v -> {
                Matcher m = varPattern.matcher(v);
                while (m.find()) usedVars.add(m.group(1).trim());
            });

            for (String varPath : usedVars) {
                Object val = resolveVariableValue(varPath, allVars);
                if (val instanceof List) {
                    firstCollectionPath = varPath;
                    collectionValues = (List<Object>) val;
                    break;
                } else if (val != null && val.getClass().isArray()) {
                    firstCollectionPath = varPath;
                    collectionValues = Arrays.asList((Object[]) val);
                    break;
                } else if (val instanceof org.graalvm.polyglot.Value && ((org.graalvm.polyglot.Value) val).hasArrayElements()) {
                    firstCollectionPath = varPath;
                    org.graalvm.polyglot.Value polyVal = (org.graalvm.polyglot.Value) val;
                    collectionValues = new ArrayList<>();
                    for (int i = 0; i < polyVal.getArraySize(); i++) {
                        collectionValues.add(polyVal.getArrayElement(i));
                    }
                    break;
                }
            }

            if (firstCollectionPath != null) {
                // If it's a JsonPath like $.cars..make, we want to iterate over the 'cars' variable instead
                // but actually, our current logic uses the result of the JsonPath as the collection.
                // We need to make sure that for each iteration, we can resolve other variables correctly.
                
                requestJS.getVariables().set("__iterationVarPath", firstCollectionPath);
                report.append("Iterating over variable `").append(firstCollectionPath).append("` (")
                        .append(collectionValues.size()).append(" items)\n\n");
                for (int i = 0; i < collectionValues.size(); i++) {
                    Object currentVal = collectionValues.get(i);
                    
                    report.append("### Execution ").append(i + 1).append(" (`").append(firstCollectionPath)
                            .append("` = `").append(currentVal).append("`)\n\n");
                    
                    requestJS.setIteration(i);
                    executeSingleRequest(test, allVars, requestJS, httpClientJS, report, context);
                    report.append("\n");
                }
                requestJS.getVariables().remove("__iterationVarPath");
            } else {
                requestJS.setIteration(0);
                executeSingleRequest(test, allVars, requestJS, httpClientJS, report, context);
            }

        } catch (Exception e) {
            report.append("❌ **Error during execution:** ").append(e.getMessage()).append("\n");
            log.error("Error during execution", e);
        } finally {
            appendResults(httpClientJS, report);
        }
    }

    private void executeSingleRequest(HttpTest test, Map<String, Object> allVars, RequestJS requestJS, HttpClientJS httpClientJS, StringBuilder report, org.graalvm.polyglot.Context context) {
        try {
        String iterationVarPath = (String) requestJS.getVariables().get("__iterationVarPath");
        int iterationIndex = requestJS.getIteration();

        // Collect template values for request.templateValue(index)
        List<Object> templateValues = new ArrayList<>();
        Pattern varPattern = Pattern.compile("\\{\\{(.+?)}}");
        
        if (test.getUrl() != null) {
            Matcher m = varPattern.matcher(test.getUrl());
            while (m.find()) {
                String varPath = m.group(1).trim();
                templateValues.add(resolveVariableValueForIteration(varPath, allVars, iterationVarPath, iterationIndex));
            }
        }
        if (test.getBody() != null) {
            Matcher m = varPattern.matcher(test.getBody());
            while (m.find()) {
                String varPath = m.group(1).trim();
                templateValues.add(resolveVariableValueForIteration(varPath, allVars, iterationVarPath, iterationIndex));
            }
        }
        // Headers are a map, and their order might not be strictly preserved or defined for templateValue
        // but the issue description shows it for body. Usually, it's URL then body.
        requestJS.setTemplateValues(templateValues);

        String url = resolveVariables(test.getUrl(), allVars, iterationVarPath, iterationIndex);
            if (url == null || url.trim().isEmpty()) {
                log.error("Request URL is missing for test: {}. Method: {}, Headers: {}, Body: {}", test.getName(), test.getMethod(), test.getHeaders(), test.getBody());
                throw new RuntimeException("Request URL is missing. Check if the .http file has a valid request line (e.g., GET http://...)");
            }
            String method = test.getMethod();
            if (method == null || method.trim().isEmpty()) {
                throw new RuntimeException("Request method is missing. Check if the .http file has a valid request line.");
            }
            Map<String, String> headers = new HashMap<>();
            // Apply global headers first
            httpClientJS.global.headers.all().forEach((k, v) -> {
                headers.put(k, resolveVariables(v, allVars, iterationVarPath, iterationIndex));
            });

            test.getHeaders().forEach((k, v) -> {
                String resolvedValue = resolveVariables(v, allVars, iterationVarPath, iterationIndex);
                if (k.equalsIgnoreCase("Authorization") && resolvedValue != null) {
                    if (resolvedValue.startsWith("Basic ") && !resolvedValue.contains(":")) {
                        String credentials = resolvedValue.substring(6).trim();
                        // If it contains space and not already base64 (guess by space)
                        if (credentials.contains(" ")) {
                            String[] parts = credentials.split("\\s+", 2);
                            String username = parts[0];
                            String password = parts.length > 1 ? parts[1] : "";
                            String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
                            resolvedValue = "Basic " + encoded;
                        }
                    }
                }
                headers.put(k, resolvedValue);
            });
            String body = resolveVariables(test.getBody(), allVars, iterationVarPath, iterationIndex);

            if (!report.toString().endsWith("\n\n")) {
                if (report.toString().endsWith("\n")) {
                    report.append("\n");
                } else {
                    report.append("\n\n");
                }
            }
            report.append("**Request:** `").append(method).append(" ").append(url).append("`\n\n");

            if (!headers.isEmpty()) {
                report.append("**Request Headers:**\n\n");
                headers.forEach((k, v) -> {
                    String valueToDisplay = v;
                    if (k.equalsIgnoreCase("Authorization")) {
                        valueToDisplay = "************";
                    }
                    report.append("- ").append(k).append(": ").append(valueToDisplay).append("\n");
                });
                report.append("\n");
            }

            if (body != null && !body.isEmpty()) {
                report.append("**Request Body:**\n\n```json\n").append(body).append("\n```\n\n");
            }

            RestClient.Builder perRequestBuilder = builder.clone();
            if (test.getTimeout() != null || test.getConnectionTimeout() != null) {
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                if (test.getTimeout() != null) {
                    factory.setReadTimeout(test.getTimeout());
                }
                if (test.getConnectionTimeout() != null) {
                    factory.setConnectTimeout(test.getConnectionTimeout());
                }
                perRequestBuilder.requestFactory(factory);
            }
            RestClient client = perRequestBuilder.build();
            Map<String, String> maskedHeaders = new HashMap<>(headers);
            if (maskedHeaders.containsKey("Authorization")) {
                maskedHeaders.put("Authorization", "************");
            }
            log.info("Executing request: {} {} headers: {} body: {}", method, url, maskedHeaders, body);
            RestClient.RequestBodySpec requestSpec = client.method(org.springframework.http.HttpMethod.valueOf(method))
                    .uri(url);
            headers.forEach(requestSpec::header);
            
            ResponseEntity<String> responseEntity;
            RestClient.ResponseSpec responseSpec = (body != null && !body.isEmpty())
                    ? requestSpec.body(body).retrieve()
                    : requestSpec.retrieve();

            responseEntity = responseSpec
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, resp) -> {
                        // Do nothing, we want to handle all statuses manually in scripts
                    })
                    .toEntity(String.class);

            // Handle JSON body if applicable
            Object finalBody = responseEntity.getBody();
            String contentType = responseEntity.getHeaders().getContentType() != null ? responseEntity.getHeaders().getContentType().toString() : "";
            if (contentType.contains("json")) {
                try {
                    finalBody = context.eval("js", "JSON.parse").execute(responseEntity.getBody());
                } catch (Exception e) {
                    log.warn("Could not parse response body as JSON: {}", e.getMessage());
                }
            } else if (contentType.contains("xml") || contentType.contains("html")) {
                try {
                    DomJS.NodeWrapper doc = new DomJS.DOMParser().parseFromString(responseEntity.getBody(), contentType);
                    finalBody = context.eval("js", "(function(javaNode) { " +
                            "    var wrap = function(jn) { " +
                            "      if (!jn) return null; " +
                            "      var node = { " +
                            "        get nodeName() { return jn.getNodeName(); }, " +
                            "        get nodeValue() { return jn.getNodeValue(); }, " +
                            "        get nodeType() { return jn.getNodeType(); }, " +
                            "        get parentNode() { return wrap(jn.getParentNode()); }, " +
                            "        get childNodes() { return jn.getChildNodes().toArray().map(wrap); }, " +
                            "        get firstChild() { var c = jn.getChildNodes(); return c.size() > 0 ? wrap(c.get(0)) : null; }, " +
                            "        get lastChild() { var c = jn.getChildNodes(); return c.size() > 0 ? wrap(c.get(c.size()-1)) : null; }, " +
                            "        get nextSibling() { return wrap(jn.getNextSibling()); }, " +
                            "        get previousSibling() { return wrap(jn.getPreviousSibling()); }, " +
                            "        get textContent() { return jn.getTextContent(); }, " +
                            "        get xml() { return jn.getXml(); }, " +
                            "        get tagName() { return jn.getTagName(); }, " +
                            "        get id() { return jn.getId(); }, " +
                            "        get className() { return jn.getClassName(); }, " +
                            "        get isConnected() { return jn.getParentNode() !== null || jn.getNodeType() === 9; }, " +
                            "        getElementsByTagName: function(tag) { return jn.getElementsByTag(tag).toArray().map(wrap); }, " +
                            "        getElementsByClassName: function(cls) { return jn.getElementsByClass(cls).toArray().map(wrap); }, " +
                            "        getElementsByName: function(name) { return jn.getElementsByAttribute('name', name).toArray().map(wrap); }, " +
                            "        getElementById: function(id) { return wrap(jn.getElementById(id)); }, " +
                            "        createElement: function(tag) { return wrap(jn.createElement(tag)); }, " +
                            "        hasChildNodes: function() { return jn.getChildNodes().size() > 0; }, " +
                            "        cloneNode: function(deep) { return wrap(jn.node.shallowClone()); }, " + // jsoup clone deep by default, wrap needs work for deep
                            "        contains: function(other) { return false; }, " + // Simplified
                            "        isSameNode: function(other) { return other && other._jn && jn.node === other._jn.node; }, " +
                            "        isEqualNode: function(other) { return other && other._jn && jn.node.equals(other._jn.node); }, " +
                            "        toJSON: function() { return this.xml; }, " +
                            "        _jn: jn " +
                            "      }; " +
                            "      return node; " +
                            "    }; " +
                            "    return wrap(javaNode); " +
                            "})").execute(doc);
                } catch (Exception e) {
                    log.warn("Could not parse response body as DOM: {}", e.getMessage());
                }
            }

            ResponseJS responseJS = new ResponseJS(
                    responseEntity.getStatusCode().value(),
                    responseEntity.getHeaders().toSingleValueMap(),
                    finalBody,
                    responseEntity.getBody()
            );

            // 4. Response Header in report
            report.append("**Response Status:** ").append(responseJS.getStatus()).append("\n\n");

            // 5. Post-script
            if (test.getPostScript() != null && !test.getPostScript().isEmpty()) {
                setupGraalJsContext(requestJS, httpClientJS, responseJS, context);
                try {
                    log.info("Executing post-script for test: {}", test.getUrl());
                    String wrappedScript = "(function() { " + test.getPostScript() + " \n})();";
                    context.eval("js", wrappedScript);
                } catch (Exception e) {
                    log.error("Error in post-script for test {}: {}", test.getUrl(), e.getMessage());
                    report.append("❌ **Error in post-script:** ").append(e.getMessage()).append("\n");
                }
                
                // Propagate global variables back to requestJS after post-script
                httpClientJS.getGlobal().all().forEach((k, v) -> {
                    if (v != null) {
                        requestJS.getVariables().set(k, v);
                    }
                });
            }
            appendResults(httpClientJS, report);
        } catch (Exception e) {
            report.append("❌ **Error during execution:** ").append(e.getMessage()).append("\n");
            log.error("Error during execution", e);
            appendResults(httpClientJS, report);
        }
    }

    private void executeSql(String sqlScript, RequestJS requestJS, HttpClientJS httpClientJS, StringBuilder report) {
        String sql = sqlScript;
        // Basic parameter substitution in SQL
        for (Map.Entry<String, Object> entry : requestJS.getVariables().all().entrySet()) {
            sql = sql.replace(":" + entry.getKey(), entry.getValue().toString());
        }

        // Very basic SQL parser for the extended format
        String actualSql = sql;
        String[] sqlLines = sql.split("\\r?\\n");
        for (String line : sqlLines) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("### Query =")) {
                actualSql = trimmedLine.substring(trimmedLine.indexOf("=") + 1).trim();
                // Remove potential wrapping quotes
                if (actualSql.startsWith("\"") && actualSql.endsWith("\"")) {
                    actualSql = actualSql.substring(1, actualSql.length() - 1);
                }
                break;
            }
        }

        List<Map<String, Object>> resultSet = new ArrayList<>();
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(actualSql);
        SqlRowSetMetaData metaData = rowSet.getMetaData();
        String[] columnNames = metaData.getColumnNames();

        while (rowSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String col : columnNames) {
                row.put(col, rowSet.getObject(col));
            }
            resultSet.add(row);
        }

        httpClientJS.getGlobal().set("ResultSet", resultSet);
        httpClientJS.getGlobal().set("ResultSetColumns", columnNames);
        report.append("**SQL Query executed.** Returned ").append(resultSet.size()).append(" rows.\n\n");
    }

    private void setupGraalJsContext(RequestJS requestJS, HttpClientJS httpClientJS, ResponseJS responseJS, org.graalvm.polyglot.Context context) {
        context.getBindings("js").putMember("request", requestJS);
        context.getBindings("js").putMember("__client", httpClientJS);
        context.getBindings("js").putMember("__utils", utilsJS);
        context.getBindings("js").putMember("__crypto", cryptoJS);
        context.getBindings("js").putMember("__subtle", subtleCryptoJS);
        context.getBindings("js").putMember("__jwt", jwtJS);
        context.getBindings("js").putMember("__UrlSearchParams", UrlSearchParamsJS.class);
        if (responseJS != null) {
            context.getBindings("js").putMember("response", responseJS);
        } else {
            // Remove previous response if any
            context.getBindings("js").removeMember("response");
        }

        // Functional interface for jsonPath to be accessible from JS
        context.getBindings("js").putMember("__jsonPath", (java.util.function.BiFunction<Object, String, Object>) (json, path) -> {
            try {
                if (json instanceof String) {
                    return JsonPath.read((String) json, path);
                } else {
                    return JsonPath.read(json, path);
                }
            } catch (Exception e) {
                return null;
            }
        });

        context.getBindings("js").putMember("__sqlQuery", (java.util.function.Function<String, Map<String, Object>>) (sql) -> {
            try {
                List<Map<String, Object>> data = new ArrayList<>();
                SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql);
                SqlRowSetMetaData metaData = rowSet.getMetaData();
                String[] columnNames = metaData.getColumnNames();

                while (rowSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (String col : columnNames) {
                        row.put(col, rowSet.getObject(col));
                    }
                    data.add(row);
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("data", data);
                result.put("columns", columnNames);
                return result;
            } catch (Exception e) {
                throw new RuntimeException("SQL execution error: " + e.getMessage(), e);
            }
        });

        context.getBindings("js").putMember("__domParser", new DomJS.DOMParser());

        // Map client.assert and other methods
        context.eval("js", "var client = { " +
                "global: { " +
                "  set: function(name, value) { __client.getGlobal().set(name, value); }," +
                "  get: function(name) { return __client.getGlobal().get(name); }," +
                "  isEmpty: function() { return __client.getGlobal().isEmpty(); }," +
                "  clear: function(name) { __client.getGlobal().clear(name); }," +
                "  clearAll: function() { __client.getGlobal().clearAll(); }," +
                "  all: function() { return __client.getGlobal().all(); }," +
                "  headers: { " +
                "    set: function(name, value) { __client.getGlobal().headers.set(name, value); }," +
                "    clear: function(name) { __client.getGlobal().headers.clear(name); }," +
                "    all: function() { return __client.getGlobal().headers.all(); }" +
                "  }" +
                "}," +
                "test: function(name, callback) { __client.test(name, callback); }," +
                "assert: function(condition, message) { __client.assertCondition(condition, message); }," +
                "log: function(message) { __client.log(message); }," +
                "markdown: function(content) { __client.markdown(content); }," +
                "variables: { " +
                "  global: { " +
                "    set: function(name, value) { __client.getGlobal().set(name, value); }," +
                "    get: function(name) { return __client.getGlobal().get(name); }," +
                "    isEmpty: function() { return __client.getGlobal().isEmpty(); }," +
                "    clear: function(name) { __client.getGlobal().clear(name); }," +
                "    clearAll: function() { __client.getGlobal().clearAll(); }," +
                "    all: function() { return __client.getGlobal().all(); }" +
                "  }" +
                "}," +
                "sqlQuery: function(sql) { return __sqlQuery(sql); }" +
                "};" +
                "(function() { " +
                "  var originalParse = JSON.parse; " +
                "  JSON.parse = function(text, reviver) { " +
                "    if (typeof text === 'object' && text !== null) return text; " +
                "    return originalParse(text, reviver); " +
                "  }; " +
                "})();" +
                "var sleep = function(ms) { return __utils.sleep(ms); };" +
                "var setTimeout = function(cb, ms) { return __utils.setTimeout(cb, ms); };" +
                "var clearTimeout = function(id) { return __utils.clearTimeout(id); };" +
                "var jsonPath = function(json, path) { return __jsonPath(json, path); };" +
                "var btoa = function(s) { return __utils.btoa(s); };" +
                "var atob = function(s) { return __utils.atob(s); };" +
                "var Window = { btoa: btoa, atob: atob };" +
                "var URLSearchParams = function(init) { " +
                "  var javaObj = new __UrlSearchParams(init); " +
                "  this.append = function(n, v) { javaObj.append(n, v); }; " +
                "  this.delete = function(n) { javaObj.delete(n); }; " +
                "  this.get = function(n) { return javaObj.get(n); }; " +
                "  this.getAll = function(n) { return javaObj.getAll(n); }; " +
                "  this.has = function(n) { return javaObj.has(n); }; " +
                "  this.set = function(n, v) { javaObj.set(n, v); }; " +
                "  this.sort = function() { javaObj.sort(); }; " +
                "  this.toString = function() { return javaObj.toString(); }; " +
                "  this.keys = function*() { var k = javaObj.keys(); for(var i=0; i<k.size(); i++) yield k.get(i); }; " +
                "  this.values = function*() { var v = javaObj.values(); for(var i=0; i<v.size(); i++) yield v.get(i); }; " +
                "  this.entries = function*() { var e = javaObj.entries(); for(var i=0; i<e.size(); i++) yield e.get(i); }; " +
                "  this[Symbol.iterator] = this.entries; " +
                "};" +
                "var DOMParser = function() { " +
                "  this.parseFromString = function(s, t) { " +
                "    var javaNode = __domParser.parseFromString(s, t); " +
                "    var wrap = function(jn) { " +
                "      if (!jn) return null; " +
                "      var node = { " +
                "        get nodeName() { return jn.getNodeName(); }, " +
                "        get nodeValue() { return jn.getNodeValue(); }, " +
                "        get nodeType() { return jn.getNodeType(); }, " +
                "        get parentNode() { return wrap(jn.getParentNode()); }, " +
                "        get childNodes() { return jn.getChildNodes().toArray().map(wrap); }, " +
                "        get firstChild() { var c = jn.getChildNodes(); return c.size() > 0 ? wrap(c.get(0)) : null; }, " +
                "        get lastChild() { var c = jn.getChildNodes(); return c.size() > 0 ? wrap(c.get(c.size()-1)) : null; }, " +
                "        get nextSibling() { return wrap(jn.getNextSibling()); }, " +
                "        get previousSibling() { return wrap(jn.getPreviousSibling()); }, " +
                "        get textContent() { return jn.getTextContent(); }, " +
                "        get tagName() { return jn.getTagName(); }, " +
                "        get id() { return jn.getId(); }, " +
                "        get className() { return jn.getClassName(); }, " +
                "        get isConnected() { return jn.getParentNode() !== null || jn.getNodeType() === 9; }, " +
                "        getElementsByTagName: function(tag) { return jn.getElementsByTag(tag).toArray().map(wrap); }, " +
                "        getElementsByClassName: function(cls) { return jn.getElementsByClass(cls).toArray().map(wrap); }, " +
                "        getElementsByName: function(name) { return jn.getElementsByAttribute('name', name).toArray().map(wrap); }, " +
                "        getElementById: function(id) { return wrap(jn.getElementById(id)); }, " +
                "        createElement: function(tag) { return wrap(jn.createElement(tag)); }, " +
                "        xpath: function(expression) { return jn.xpath(expression).toArray().map(wrap); }, " +
                "        hasChildNodes: function() { return jn.getChildNodes().size() > 0; }, " +
                "        cloneNode: function(deep) { var n = jn.getNode(); return wrap(n.shallowClone ? n.shallowClone() : n.cloneNode(deep)); }, " +
                "        contains: function(other) { return false; }, " +
                "        isSameNode: function(other) { return other && other._jn && (jn.getNode() === other._jn.getNode()); }, " +
                "        isEqualNode: function(other) { return other && other._jn && (jn.getNode().equals(other._jn.getNode())); }, " +
                "        _jn: jn " +
                "      }; " +
                "      return node; " +
                "    }; " +
                "    return wrap(javaNode); " +
                "  }; " +
                "};" +
                "var crypto = { " +
                "  sha256: function() { return __crypto.sha256(); }," +
                "  sha512: function() { return __crypto.sha512(); }," +
                "  hmac: { " +
                "    sha256: function() { return __crypto.hmac.sha256(); }," +
                "    sha512: function() { return __crypto.hmac.sha512(); }," +
                "    sha3: function(bits) { return __crypto.hmac.sha3(bits); }" +
                "  }," +
                "  subtle: { " +
                "    generateKey: function(alg, ext, usages) { return __subtle.generateKey(alg, ext, usages); }," +
                "    importKey: function(fmt, data, alg, ext, usages) { return __subtle.importKey(fmt, data, alg, ext, usages); }," +
                "    exportKey: function(fmt, key) { return __subtle.exportKey(fmt, key); }," +
                "    sign: function(alg, key, data) { return __subtle.sign(alg, key, data); }," +
                "    verify: function(alg, key, sig, data) { return __subtle.verify(alg, key, sig, data); }," +
                "    encrypt: function(alg, key, data) { return __subtle.encrypt(alg, key, data); }," +
                "    decrypt: function(alg, key, data) { return __subtle.decrypt(alg, key, data); }" +
                "  }" +
                "};" +
                "var jwt = { " +
                "  sign: function(payload, key, options) { return __jwt.sign(payload, key, options || {}); }," +
                "  verify: function(token, key, options) { return __jwt.verify(token, key, options || {}); }," +
                "  decode: function(token) { return __jwt.decode(token); }" +
                "};" +
                "function string2byteArray(str) { " +
                "  var arr = new Uint8Array(str.length); " +
                "  for (var i = 0; i < str.length; i++) arr[i] = str.charCodeAt(i); " +
                "  return arr; " +
                "}");
    }

    private void appendResults(HttpClientJS httpClientJS, StringBuilder report) {
        if (!httpClientJS.getMarkdownEntries().isEmpty()) {
            report.append("\n");
            for (String markdownEntry : httpClientJS.getMarkdownEntries()) {
                report.append(markdownEntry).append("\n");
            }
            report.append("\n");
            httpClientJS.getMarkdownEntries().clear();
        }

        if (!httpClientJS.getTestResults().isEmpty()) {
            report.append("**Test Results:**\n");
            for (String result : httpClientJS.getTestResults()) {
                report.append("- ").append(result).append("\n");
            }
            report.append("\n");
            httpClientJS.getTestResults().clear();
        }

        if (!httpClientJS.getLogs().isEmpty()) {
            report.append("**Logs:**\n");
            for (String logEntry : httpClientJS.getLogs()) {
                if (logEntry.contains("\n")) {
                    report.append("- ").append(logEntry.replace("\n", "\n  ")).append("\n");
                } else {
                    report.append("- ").append(logEntry).append("\n");
                }
            }
            report.append("\n");
            httpClientJS.getLogs().clear();
        }
    }

    private String resolveVariables(String text, Map<String, Object> variables, String iterationVarPath, int iterationIndex) {
        if (text == null) return null;
        String result = text;

        // Pattern for {{varName}}
        Pattern varPattern = Pattern.compile("\\{\\{(.+?)}}");
        Matcher m = varPattern.matcher(result);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String varPath = m.group(1).trim();
            Object valueObj = resolveVariableValueForIteration(varPath, variables, iterationVarPath, iterationIndex);
            String value = valueObj != null ? valueObj.toString() : "";
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        result = sb.toString();

        // Resolve dynamic variables starting with $
        result = resolveDynamicVariables(result);

        return result;
    }

    private Object resolveVariableValueForIteration(String varPath, Map<String, Object> variables, String iterationVarPath, int iterationIndex) {
        if (iterationVarPath != null && varPath.startsWith(iterationVarPath.substring(0, iterationVarPath.lastIndexOf("..") + 2))) {
            // If this variable starts with the same path as the iterated variable,
            // we should try to resolve it for the current iteration.
            try {
                Object fullList = JsonPath.read(variables, varPath);
                if (fullList instanceof List) {
                    List<?> list = (List<?>) fullList;
                    if (iterationIndex < list.size()) {
                        return list.get(iterationIndex);
                    }
                }
            } catch (Exception e) {
                // fall back to normal resolution
            }
        }
        return resolveVariableValue(varPath, variables);
    }

    private String resolveVariables(String text, Map<String, Object> variables) {
        return resolveVariables(text, variables, null, 0);
    }

    private Object resolveVariableValue(String varName, Map<String, Object> variables) {
        if (varName.startsWith("$")) {
            // Try as JsonPath
            try {
                // Find which variable it refers to. e.g. $.cars..make -> refers to 'cars'
                // But JsonPath expects the whole object.
                // Our variables map contains 'cars' as a List or Array.
                // If it starts with $ it's a JsonPath on the variables context? 
                // Usually in IntelliJ HTTP Client, {{$.cars..make}} means use JsonPath on the variables.
                // We can wrap the variables map into something JsonPath can read.
                Object result = JsonPath.read(variables, varName);
                if (result instanceof List) {
                    List<?> array = (List<?>) result;
                    if (array.size() == 1) {
                        return array.get(0);
                    } else if (array.isEmpty()) {
                        return null;
                    }
                }
                return result;
            } catch (Exception e) {
                log.debug("Failed to resolve JsonPath variable: {}", varName);
                return variables.get(varName);
            }
        }
        return variables.get(varName);
    }

    private String resolveDynamicVariables(String text) {
        if (text == null) return null;
        
        // Pattern for {{$...}}
        Pattern pattern = Pattern.compile("\\{\\{\\$(.+?)}}");
        Matcher matcher = pattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            String replacement = null;

            if ("uuid".equals(varName) || "random.uuid".equals(varName)) {
                replacement = UUID.randomUUID().toString();
            } else if ("timestamp".equals(varName)) {
                replacement = String.valueOf(Instant.now().getEpochSecond());
            } else if ("isoTimestamp".equals(varName)) {
                replacement = Instant.now().toString();
            } else if ("randomInt".equals(varName)) {
                replacement = String.valueOf(random.nextInt(1001));
            } else if (varName.startsWith("random.integer")) {
                replacement = resolveRandomInteger(varName, random);
            } else if (varName.startsWith("random.float")) {
                replacement = resolveRandomFloat(varName, random);
            } else if (varName.startsWith("random.alphabetic")) {
                replacement = resolveRandomAlphabetic(varName, random);
            } else if (varName.startsWith("random.alphanumeric")) {
                replacement = resolveRandomAlphanumeric(varName, random);
            } else if (varName.startsWith("random.hexadecimal")) {
                replacement = resolveRandomHexadecimal(varName, random);
            } else if ("random.email".equals(varName)) {
                replacement = generateRandomEmail(random);
            }

            if (replacement != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String resolveRandomInteger(String varName, Random random) {
        Pattern p = Pattern.compile("random\\.integer\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)");
        Matcher m = p.matcher(varName);
        if (m.find()) {
            int from = Integer.parseInt(m.group(1));
            int to = Integer.parseInt(m.group(2));
            if (to > from) {
                return String.valueOf(from + random.nextInt(to - from));
            }
        }
        return String.valueOf(random.nextInt(1001));
    }

    private String resolveRandomFloat(String varName, Random random) {
        Pattern p = Pattern.compile("random\\.float\\s*\\(\\s*([\\d.]+)\\s*,\\s*([\\d.]+)\\s*\\)");
        Matcher m = p.matcher(varName);
        if (m.find()) {
            double from = Double.parseDouble(m.group(1));
            double to = Double.parseDouble(m.group(2));
            if (to > from) {
                return String.valueOf(from + (to - from) * random.nextDouble());
            }
        }
        return String.valueOf(random.nextDouble() * 1000);
    }

    private String resolveRandomAlphabetic(String varName, Random random) {
        int length = extractLength(varName, 10);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        return generateRandomString(chars, length, random);
    }

    private String resolveRandomAlphanumeric(String varName, Random random) {
        int length = extractLength(varName, 10);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_";
        return generateRandomString(chars, length, random);
    }

    private String resolveRandomHexadecimal(String varName, Random random) {
        int length = extractLength(varName, 10);
        String chars = "0123456789abcdef";
        return generateRandomString(chars, length, random);
    }

    private String generateRandomString(String chars, int length, Random random) {
        if (length <= 0) length = 10;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private int extractLength(String varName, int defaultLength) {
        Pattern p = Pattern.compile("\\(\\s*(\\d+)\\s*\\)");
        Matcher m = p.matcher(varName);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return defaultLength;
    }

    private String generateRandomEmail(Random random) {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        return generateRandomString(chars, 8, random) + "@" + generateRandomString(chars, 5, random) + ".com";
    }
}
