package one.dastec.restunittest.services;

import one.dastec.restunittest.config.AppProperties;
import one.dastec.restunittest.js.HttpClientJS;
import one.dastec.restunittest.js.RequestJS;
import one.dastec.restunittest.models.HttpTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.web.client.RestClient;

import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class RestTestServiceTest {

    private RestTestService restTestService;
    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    private RestClient.Builder builder;
    private GraalJsService graalJsService;
    private RestClient restClient;
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.ResponseSpec responseSpec;

    private AppProperties appProperties;
    private CryptoService cryptoService;

    @BeforeEach
    public void setUp() {
        dataSource = Mockito.mock(DataSource.class);
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        builder = Mockito.mock(RestClient.Builder.class);
        graalJsService = new GraalJsService(); // Use real one for JS execution
        restClient = Mockito.mock(RestClient.class);
        requestBodyUriSpec = Mockito.mock(RestClient.RequestBodyUriSpec.class);
        responseSpec = Mockito.mock(RestClient.ResponseSpec.class);
        appProperties = new AppProperties();
        appProperties.getEnvironment().setName("TestEnv");
        cryptoService = new CryptoService();

        when(builder.clone()).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);
        when(restClient.method(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.header(anyString(), anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        
        restTestService = new RestTestService(dataSource, jdbcTemplate, builder, graalJsService, appProperties, cryptoService);
    }

    @Test
    public void testRunTest() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>("OK", HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        SqlRowSet rowSet = Mockito.mock(SqlRowSet.class);
        SqlRowSetMetaData metaData = Mockito.mock(SqlRowSetMetaData.class);
        when(jdbcTemplate.queryForRowSet(anyString())).thenReturn(rowSet);
        when(rowSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnNames()).thenReturn(new String[]{"id"});
        when(rowSet.next()).thenReturn(true, false);
        when(rowSet.getObject("id")).thenReturn(123);

        String result = restTestService.runTest("test");
        
        System.out.println(result);
        assertTrue(result.contains("# Test Report"));
        assertTrue(result.contains("## Test 1"));
        assertTrue(result.contains("✅ Status is 200"));
        assertTrue(result.contains("✅ SQL result is correct"));
    }

    @Test
    public void testExtendedSql() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>("OK", HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        SqlRowSet rowSet = Mockito.mock(SqlRowSet.class);
        SqlRowSetMetaData metaData = Mockito.mock(SqlRowSetMetaData.class);
        when(jdbcTemplate.queryForRowSet("SELECT * FROM extended_table")).thenReturn(rowSet);
        when(rowSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnNames()).thenReturn(new String[]{"id"});
        when(rowSet.next()).thenReturn(true, false);
        when(rowSet.getObject("id")).thenReturn(456);

        String result = restTestService.runTest("extended_sql");

        System.out.println(result);
        assertTrue(result.contains("✅ SQL result exists"), "Extended SQL test should pass");
    }

    @Test
    public void testNewSqlFormat() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>("OK", HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        SqlRowSet rowSet = Mockito.mock(SqlRowSet.class);
        SqlRowSetMetaData metaData = Mockito.mock(SqlRowSetMetaData.class);
        when(jdbcTemplate.queryForRowSet("SELECT * FROM new_format_table")).thenReturn(rowSet);
        when(rowSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnNames()).thenReturn(new String[]{"id"});
        when(rowSet.next()).thenReturn(true, false);
        when(rowSet.getObject("id")).thenReturn(789);

        String result = restTestService.runTest("new_sql_format");

        System.out.println(result);
        assertTrue(result.contains("✅ SQL result from new format"), "New SQL format test should pass");
    }

    @Test
    public void testSqlMarkdown() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>("OK", HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        SqlRowSet rowSet = Mockito.mock(SqlRowSet.class);
        SqlRowSetMetaData metaData = Mockito.mock(SqlRowSetMetaData.class);
        
        when(jdbcTemplate.queryForRowSet(anyString())).thenReturn(rowSet);
        when(rowSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnNames()).thenReturn(new String[]{"id", "name"});
        
        when(rowSet.next()).thenReturn(true, false);
        when(rowSet.getObject("id")).thenReturn(1);
        when(rowSet.getObject("name")).thenReturn("Alice");

        String result = restTestService.runTest("sql_markdown");

        System.out.println(result);
        assertTrue(result.contains("✅ Table generated"), "SQL Markdown test should pass");
        assertTrue(result.contains("| id | name |"), "Report should contain the table header");
        assertTrue(result.contains("| 1 | Alice |"), "Report should contain the table data");
    }

    @Test
    public void testGetTestSource() {
        String source = restTestService.getTestSource("test");
        assertTrue(source.contains("GET http://localhost:8080/api/test"), "Source should contain the request");
        assertTrue(source.contains("### Test 1"), "Source should contain the test name");
    }

    @Test
    public void testRunTestWithContent() {
        String content = "### Custom Test\nGET http://example.com";
        ResponseEntity<String> responseEntity = new ResponseEntity<>("OK", HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        String result = restTestService.runTestWithContent("Custom Test", content);
        assertTrue(result.contains("# Test Report: Custom Test"));
        assertTrue(result.contains("Date: "), "Report should include the execution date and time");
        assertTrue(result.contains("## Custom Test"));
    }
    @Test
    void testRunTestWithGlobals() {
        String content = "GET {{baseUrl}}/api/test\nAuthorization: Bearer {{token}}";
        Map<String, Object> globals = new HashMap<>();
        globals.put("token", "my-secret-token");
        
        String report = restTestService.runTestWithContent("Test with Globals", content, globals);
        
        assertTrue(report.contains("Test Report: Test with Globals"));
    }

    @Test
    void testAppTestGlobals() {
        appProperties.getTestGlobals().put("globalVar", "globalValue");
        String content = "### Test Global\nGET http://localhost:8080/api/test?var={{globalVar}}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>("OK", HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        String report = restTestService.runTestWithContent("Test with App Globals", content);

        assertTrue(report.contains("Test Report: Test with App Globals"));
        Mockito.verify(requestBodyUriSpec).uri(contains("var=globalValue"));
    }

    @Test
    public void testEnvironmentNamePrioritization() {
        String content = "### Env Test\nGET http://example.com";
        ResponseEntity<String> responseEntity = new ResponseEntity<>("OK", HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        // 1. No environment name in globals, should use appProperties
        appProperties.getEnvironment().setName("ConfigEnv");
        String result1 = restTestService.runTestWithContent("Env Test 1", content);
        assertTrue(result1.contains("Environment: ConfigEnv"));

        // 2. Environment name in globals, should prioritize it
        Map<String, Object> globals = new HashMap<>();
        globals.put("__ENV_NAME__", "BrowserEnv");
        String result2 = restTestService.runTestWithContent("Env Test 2", content, globals);
        assertTrue(result2.contains("Environment: BrowserEnv"));
        
        // Check if JS variable environmentName is also updated
        String contentWithJS = "### Env JS Test\nGET http://example.com\n> {%\n client.test('env name', function() { client.assert(client.global.get('environmentName') === 'BrowserEnv'); });\n%}";
        String result3 = restTestService.runTestWithContent("Env JS Test", contentWithJS, globals);
        assertTrue(result3.contains("✅ env name"));
    }

    @Test
    void testEncryptedVariables() {
        String secret = "my-secret-password";
        String encrypted = "{enc}" + cryptoService.encrypt(secret);
        
        Map<String, Object> globals = new HashMap<>();
        globals.put("dbPassword", encrypted);
        
        String content = "### Test Encrypted\nGET http://localhost:8080/api/test?pass={{dbPassword}}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>("OK", HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        String report = restTestService.runTestWithContent("Test with Encrypted", content, globals);

        assertTrue(report.contains("Test Report: Test with Encrypted"));
        Mockito.verify(requestBodyUriSpec).uri(contains("pass=my-secret-password"));
    }
}
