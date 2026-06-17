package one.dastec.restunittest.services;

import one.dastec.restunittest.config.AppProperties;
import one.dastec.restunittest.js.HttpClientJS;
import one.dastec.restunittest.js.RequestJS;
import one.dastec.restunittest.js.ResponseJS;
import one.dastec.restunittest.models.HttpTest;
import org.graalvm.polyglot.Value;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class RestTestService {

    private static final Logger log = LoggerFactory.getLogger(RestTestService.class);
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final RestClient.Builder builder;
    private final GraalJsService graalJsService;
    private final AppProperties appProperties;


    public RestTestService(DataSource dataSource, JdbcTemplate jdbcTemplate, RestClient.Builder builder, GraalJsService graalJsService, AppProperties appProperties) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.builder = builder;
        this.graalJsService = graalJsService;
        this.appProperties = appProperties;
    }

    public String runTest(String testPath) {
        Resource testFile;
        if (testPath.startsWith("/") || (testPath.length() > 1 && testPath.charAt(1) == ':')) {
            testFile = new FileSystemResource(testPath);
        } else {
            testFile = new ClassPathResource(testPath);
        }
        if (!testFile.exists()) {
            log.error("Test not found: {}", testPath);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,  "Test not found: " + testPath);
        }
        try {
            var content = testFile.getContentAsString(StandardCharsets.UTF_8);
            List<HttpTest> tests = parseHttpFile(content);
            StringBuilder report = new StringBuilder();
            if (appProperties != null && appProperties.getEnvironment() != null) {
                report.append("Environment: ").append(appProperties.getEnvironment().getName()).append("\n\n");
            }
            report.append("# Test Report: ").append(testPath).append("\n\n");

            RequestJS requestJS = new RequestJS();
            HttpClientJS httpClientJS = new HttpClientJS();

            // Expose app properties to JS and populate variables
            if (appProperties != null && appProperties.getEnvironment() != null) {
                graalJsService.putMember("environment", appProperties.getEnvironment());
                String baseUrl = appProperties.getEnvironment().getBaseUrl();
                if (baseUrl != null) {
                    requestJS.getVariables().set("baseUrl", baseUrl);
                } else {
                    requestJS.getVariables().set("baseUrl", "{{baseUrl}}");
                }
                if (appProperties.getEnvironment().getName() != null) {
                    requestJS.getVariables().set("environmentName", appProperties.getEnvironment().getName());
                }
                // Also expose the whole app properties if needed
                graalJsService.putMember("app", appProperties);
            }

        // Load markdown.js helper into GraalJS context
        try {
            if (graalJsService.getContext().getBindings("js").getMember("Markdown") == null) {
                ClassPathResource markdownResource = new ClassPathResource("httptestfiles/markdown.js");
                if (markdownResource.exists()) {
                    String markdownJs = markdownResource.getContentAsString(StandardCharsets.UTF_8);
                    // Strip exports for non-module GraalJS eval
                    markdownJs = markdownJs.replaceAll("(?m)^export ", "");
                    graalJsService.executeScript(markdownJs);
                }
            }
        } catch (IOException e) {
            log.warn("Could not load markdown.js: {}", e.getMessage());
        }

            for (HttpTest test : tests) {
                report.append("## ").append(test.getName()).append("\n\n");
                executeTest(test, requestJS, httpClientJS, report);
                report.append("\n---\n\n");
            }

            return report.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<HttpTest> parseHttpFile(String content) {
        List<HttpTest> tests = new ArrayList<>();
        // Split by ### at the beginning of a line to avoid splitting on internal ###
        String[] blocks = content.split("(?m)^###");
        for (String block : blocks) {
            if (block.trim().isEmpty()) continue;
            tests.add(parseBlock("###"+block));
        }
        return tests;
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

        String firstLine = lines[firstNonEmptyLine].trim();
        // Check if first line is a request line (starts with HTTP method and has a URL)
        boolean firstLineIsRequest = false;
        String[] firstLineParts = firstLine.split("\\s+");
        if (firstLineParts.length >= 2 && firstLineParts.length <= 3) {
            firstLineIsRequest = firstLineParts[0].matches("^(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)$");
        }
        
        test.setName(firstLine.replaceAll("^###", "").trim());

        StringBuilder preScript = new StringBuilder();
        StringBuilder postScript = new StringBuilder();
        StringBuilder body = new StringBuilder();
        Map<String, String> headers = new HashMap<>();
        String method = null;
        String url = null;

        String mode = "NONE"; // NONE, PRE, POST, SQL, REQUEST_BODY

        int startParsingFrom = firstLineIsRequest ? firstNonEmptyLine : firstNonEmptyLine + 1;

        StringBuilder currentScript = new StringBuilder();

        for (int i = startParsingFrom; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();

            if (trimmedLine.startsWith("< {%")) {
                if (!currentScript.toString().trim().isEmpty()) {
                    if (mode.equals("PRE")) test.getPreActions().add(new HttpTest.PreAction("JS", currentScript.toString().trim()));
                    if (mode.equals("SQL")) test.getPreActions().add(new HttpTest.PreAction("SQL", currentScript.toString().trim()));
                }
                currentScript = new StringBuilder();
                mode = "PRE";
                continue;
            } else if (trimmedLine.startsWith("> {%SQL") || trimmedLine.startsWith("# < SQL")) {
                if (!currentScript.toString().trim().isEmpty()) {
                    if (mode.equals("PRE")) test.getPreActions().add(new HttpTest.PreAction("JS", currentScript.toString().trim()));
                    if (mode.equals("SQL")) test.getPreActions().add(new HttpTest.PreAction("SQL", currentScript.toString().trim()));
                }
                currentScript = new StringBuilder();
                mode = "SQL";
                continue;
            } else if (trimmedLine.startsWith("> {%")) {
                if (!currentScript.toString().trim().isEmpty()) {
                    if (mode.equals("PRE")) test.getPreActions().add(new HttpTest.PreAction("JS", currentScript.toString().trim()));
                    if (mode.equals("SQL")) test.getPreActions().add(new HttpTest.PreAction("SQL", currentScript.toString().trim()));
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
                    if (method != null && mode.equals("NONE")) {
                        mode = "REQUEST_BODY";
                    }
                    continue;
                }
                if (method == null) {
                    String[] parts = trimmedLine.split("\\s+");
                    if (parts.length >= 2 && parts[0].matches("^(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)$")) {
                        method = parts[0];
                        url = parts[1];
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
                body.append(line).append("\n");
            }
        }

        test.setPreScript(preScript.toString().trim());
        test.setPostScript(postScript.toString().trim());
        test.setMethod(method);
        test.setUrl(url);
        test.setHeaders(headers);
        test.setBody(body.toString().trim());

        return test;
    }

    private void executeTest(HttpTest test, RequestJS requestJS, HttpClientJS httpClientJS, StringBuilder report) {
        try {
            // 1. Pre-actions (JS and SQL in order)
            for (HttpTest.PreAction action : test.getPreActions()) {
                if ("SQL".equals(action.getType())) {
                    executeSql(action.getContent(), requestJS, httpClientJS, report);
                } else if ("JS".equals(action.getType())) {
                    setupGraalJsContext(requestJS, httpClientJS, null);
                    try {
                        graalJsService.executeScript("(function() {\n" + action.getContent() + "\n})()");
                    } catch (Exception e) {
                        report.append("❌ **Error in pre-script:** ").append(e.getMessage()).append("\n");
                    }
                    appendResults(httpClientJS, report);
                }
            }

            // 2. Resolve variables
            Map<String, Object> allVars = new HashMap<>(requestJS.getVariables().all());
            httpClientJS.getGlobal().all().forEach((k, v) -> {
                if (v != null) {
                    allVars.put(k, v);
                }
            });
            

            String url = resolveVariables(test.getUrl(), allVars);
            if (url == null || url.trim().isEmpty()) {
                throw new RuntimeException("Request URL is missing. Check if the .http file has a valid request line (e.g., GET http://...)");
            }
            String method = test.getMethod();
            if (method == null || method.trim().isEmpty()) {
                throw new RuntimeException("Request method is missing. Check if the .http file has a valid request line.");
            }
            Map<String, String> headers = new HashMap<>();
            test.getHeaders().forEach((k, v) -> headers.put(k, resolveVariables(v, allVars)));
            String body = resolveVariables(test.getBody(), allVars);

            report.append("**Request:** `").append(method).append(" ").append(url).append("`\n\n");

            // 3. Execute HTTP Request
            RestClient client = builder.build();
            RestClient.RequestBodySpec requestSpec = client.method(org.springframework.http.HttpMethod.valueOf(method))
                    .uri(url);
            headers.forEach(requestSpec::header);
            
            ResponseEntity<String> responseEntity;
            if (body != null && !body.isEmpty()) {
                responseEntity = requestSpec.body(body).retrieve().toEntity(String.class);
            } else {
                responseEntity = requestSpec.retrieve().toEntity(String.class);
            }

            ResponseJS responseJS = new ResponseJS(
                    responseEntity.getStatusCode().value(),
                    responseEntity.getHeaders().toSingleValueMap(),
                    responseEntity.getBody()
            );

            // 4. Response Header in report
            report.append("**Response Status:** ").append(responseJS.getStatus()).append("\n\n");

            // 5. Post-script
            if (test.getPostScript() != null && !test.getPostScript().isEmpty()) {
                setupGraalJsContext(requestJS, httpClientJS, responseJS);
                try {
                    graalJsService.executeScript("(function() {\n" + test.getPostScript() + "\n})()");
                } catch (Exception e) {
                    report.append("❌ **Error in post-script:** ").append(e.getMessage()).append("\n");
                }
                appendResults(httpClientJS, report);
            }

        } catch (Exception e) {
            report.append("❌ **Error during execution:** ").append(e.getMessage()).append("\n");
            e.printStackTrace();
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

    private void setupGraalJsContext(RequestJS requestJS, HttpClientJS httpClientJS, ResponseJS responseJS) {
        graalJsService.putMember("request", requestJS);
        graalJsService.putMember("__client", httpClientJS);
        if (responseJS != null) {
            graalJsService.putMember("response", responseJS);
        } else {
            // Remove previous response if any
            graalJsService.getContext().getBindings("js").removeMember("response");
        }

        // Functional interface for jsonPath to be accessible from JS
        graalJsService.putMember("__jsonPath", (java.util.function.BiFunction<Object, String, Object>) (json, path) -> {
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

        graalJsService.putMember("__sqlQuery", (java.util.function.Function<String, Map<String, Object>>) (sql) -> {
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

        // Map client.assert and other methods
        graalJsService.executeScript("var client = { " +
                "test: function(name, callback) { __client.test(name, callback); }," +
                "assert: function(condition, message) { __client.assertCondition(condition, message); }," +
                "log: function(message) { __client.log(message); }," +
                "markdown: function(content) { __client.markdown(content); }," +
                "global: __client.getGlobal()," +
                "variables: { global: __client.getGlobal() }," +
                "sqlQuery: function(sql) { return __sqlQuery(sql); }" +
                "};" +
                "var jsonPath = function(json, path) { return __jsonPath.apply(json, path); };");
    }

    private void appendResults(HttpClientJS httpClientJS, StringBuilder report) {
        if (!httpClientJS.getMarkdownEntries().isEmpty()) {
            report.append("\n");
            for (String markdownEntry : httpClientJS.getMarkdownEntries()) {
                report.append(markdownEntry).append("\n");
            }
            httpClientJS.getMarkdownEntries().clear();
        }

        if (!httpClientJS.getTestResults().isEmpty()) {
            report.append("**Test Results:**\n");
            for (String result : httpClientJS.getTestResults()) {
                report.append("- ").append(result).append("\n");
            }
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
            httpClientJS.getLogs().clear();
        }
    }

    private String resolveVariables(String text, Map<String, Object> variables) {
        if (text == null) return null;
        String result = text;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (entry.getKey() != null) {
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                result = result.replace("{{" + entry.getKey() + "}}", value);
            }
        }
        return result;
    }
}
