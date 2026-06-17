package one.dastec.restunittest.services;

import one.dastec.restunittest.config.AppProperties;
import one.dastec.restunittest.js.HttpClientJS;
import one.dastec.restunittest.js.RequestJS;
import one.dastec.restunittest.models.HttpTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
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

        when(builder.build()).thenReturn(restClient);
        when(restClient.method(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.header(anyString(), anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        
        restTestService = new RestTestService(dataSource, jdbcTemplate, builder, graalJsService, appProperties);
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
        assertTrue(result.contains("## Custom Test"));
    }
}
