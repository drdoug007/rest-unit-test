package one.dastec.restunittest.services;

import one.dastec.restunittest.js.HttpClientJS;
import one.dastec.restunittest.js.RequestJS;
import one.dastec.restunittest.js.ResponseJS;
import one.dastec.restunittest.models.HttpTest;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
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


    public RestTestService(DataSource dataSource, JdbcTemplate jdbcTemplate, RestClient.Builder builder, GraalJsService graalJsService) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.builder = builder;
        this.graalJsService = graalJsService;
    }

    public String runTest(String testPath) {
        Resource testFile = new ClassPathResource(testPath);
        if (!testFile.exists()) {
            log.error("Test not found: {}", testPath);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,  "Test not found: " + testPath);
        }
        try {
            var content = testFile.getContentAsString(StandardCharsets.UTF_8);
            List<HttpTest> tests = parseHttpFile(content);
            StringBuilder report = new StringBuilder("# Test Report: " + testPath + "\n\n");

            RequestJS requestJS = new RequestJS();
            HttpClientJS httpClientJS = new HttpClientJS();

            // Load markdown.js helper into GraalJS context
            try {
                ClassPathResource markdownResource = new ClassPathResource("httptestfiles/markdown.js");
                if (markdownResource.exists()) {
                    String markdownJs = markdownResource.getContentAsString(StandardCharsets.UTF_8);
                    // Strip exports for non-module GraalJS eval
                    markdownJs = markdownJs.replaceAll("export ", "");
                    graalJsService.executeScript(markdownJs);
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
            tests.add(parseBlock(block));
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
            firstLineIsRequest = firstLineParts[0].matches("(?i)^(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)$");
        }
        
        test.setName(firstLine);

        StringBuilder preScript = new StringBuilder();
        StringBuilder postScript = new StringBuilder();
        StringBuilder sqlScript = new StringBuilder();
        StringBuilder body = new StringBuilder();
        Map<String, String> headers = new HashMap<>();
        String method = null;
        String url = null;

        String mode = "NONE"; // NONE, PRE, POST, SQL, REQUEST_BODY

        int startParsingFrom = firstLineIsRequest ? firstNonEmptyLine : firstNonEmptyLine + 1;

        for (int i = startParsingFrom; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();

            if (trimmedLine.startsWith("< {%")) {
                mode = "PRE";
                continue;
            } else if (trimmedLine.startsWith("> {%SQL") || trimmedLine.startsWith("# < SQL")) {
                mode = "SQL";
                continue;
            } else if (trimmedLine.startsWith("> {%")) {
                mode = "POST";
                continue;
            } else if (trimmedLine.startsWith("%}") || trimmedLine.equals("# SQL")) {
                mode = "NONE";
                continue;
            }

            if (mode.equals("PRE")) {
                preScript.append(line).append("\n");
            } else if (mode.equals("POST")) {
                postScript.append(line).append("\n");
            } else if (mode.equals("SQL")) {
                sqlScript.append(line).append("\n");
            } else if (mode.equals("NONE")) {
                if (trimmedLine.isEmpty()) {
                    if (method != null && mode.equals("NONE")) {
                        mode = "REQUEST_BODY";
                    }
                    continue;
                }
                if (method == null) {
                    String[] parts = trimmedLine.split("\\s+");
                    if (parts.length >= 2) {
                        method = parts[0];
                        url = parts[1];
                    }
                } else if (trimmedLine.contains(":")) {
                    int colonIndex = trimmedLine.indexOf(":");
                    headers.put(trimmedLine.substring(0, colonIndex).trim(), trimmedLine.substring(colonIndex + 1).trim());
                }
            } else if (mode.equals("REQUEST_BODY")) {
                body.append(line).append("\n");
            }
        }

        test.setPreScript(preScript.toString().trim());
        test.setPostScript(postScript.toString().trim());
        test.setSqlScript(sqlScript.toString().trim());
        test.setMethod(method);
        test.setUrl(url);
        test.setHeaders(headers);
        test.setBody(body.toString().trim());

        return test;
    }

    private void executeTest(HttpTest test, RequestJS requestJS, HttpClientJS httpClientJS, StringBuilder report) {
        try {
            // 1. Pre-script
            if (test.getPreScript() != null && !test.getPreScript().isEmpty()) {
                graalJsService.putMember("request", requestJS);
                graalJsService.executeScript(test.getPreScript());
            }

            // 2. Resolve variables
            String url = resolveVariables(test.getUrl(), requestJS.getVariables().all());
            String method = test.getMethod();
            Map<String, String> headers = new HashMap<>();
            test.getHeaders().forEach((k, v) -> headers.put(k, resolveVariables(v, requestJS.getVariables().all())));
            String body = resolveVariables(test.getBody(), requestJS.getVariables().all());

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

            report.append("**Response Status:** ").append(responseJS.getStatus()).append("\n\n");

            // 4. SQL Execution
            if (test.getSqlScript() != null && !test.getSqlScript().isEmpty()) {
                String sql = test.getSqlScript();
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

            // 5. Post-script
            if (test.getPostScript() != null && !test.getPostScript().isEmpty()) {
                graalJsService.putMember("__client", httpClientJS);
                graalJsService.putMember("response", responseJS);
                // Functional interface for jsonPath to be accessible from JS
                graalJsService.putMember("__jsonPath", (java.util.function.BiFunction<Object, String, Object>) (json, path) -> {
                    try {
                        if (json instanceof String) {
                            return JsonPath.read((String) json, path);
                        } else {
                            // If it's already an object (e.g. from a previous JS step), 
                            // we might need to convert it back to string or handle it.
                            // Jayway JsonPath can also take an object.
                            return JsonPath.read(json, path);
                        }
                    } catch (Exception e) {
                        return null;
                    }
                });

                // Map client.assert and other methods
                graalJsService.executeScript("var client = { " +
                        "test: function(name, callback) { __client.test(name, callback); }," +
                        "assert: function(condition, message) { __client.assertCondition(condition, message); }," +
                        "log: function(message) { __client.log(message); }," +
                        "markdown: function(content) { __client.markdown(content); }," +
                        "global: __client.global" +
                        "};" +
                        "var jsonPath = function(json, path) { return __jsonPath.apply(json, path); };");
                
                try {
                    graalJsService.executeScript(test.getPostScript());
                } catch (Exception e) {
                    report.append("❌ **Error in post-script:** ").append(e.getMessage()).append("\n");
                }

                report.append("**Test Results:**\n");
                for (String result : httpClientJS.getTestResults()) {
                    report.append("- ").append(result).append("\n");
                }
                httpClientJS.getTestResults().clear();

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

                if (!httpClientJS.getMarkdownEntries().isEmpty()) {
                    report.append("\n");
                    for (String markdownEntry : httpClientJS.getMarkdownEntries()) {
                        report.append(markdownEntry).append("\n");
                    }
                    httpClientJS.getMarkdownEntries().clear();
                }
            }

        } catch (Exception e) {
            report.append("❌ **Error during execution:** ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }
    }

    private String resolveVariables(String text, Map<String, Object> variables) {
        if (text == null) return null;
        String result = text;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue().toString());
        }
        return result;
    }
}
