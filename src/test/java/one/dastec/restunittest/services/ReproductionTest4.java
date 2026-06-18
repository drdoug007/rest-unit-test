package one.dastec.restunittest.services;

import one.dastec.restunittest.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class ReproductionTest4 {

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
        graalJsService = new GraalJsService();
        restClient = Mockito.mock(RestClient.class);
        requestBodyUriSpec = Mockito.mock(RestClient.RequestBodyUriSpec.class);
        responseSpec = Mockito.mock(RestClient.ResponseSpec.class);
        appProperties = new AppProperties();

        when(builder.build()).thenReturn(restClient);
        when(restClient.method(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.header(anyString(), anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        
        restTestService = new RestTestService(dataSource, jdbcTemplate, builder, graalJsService, appProperties);
    }

    @Test
    public void testCustomTestWithMultipleBlocks() {
        String content = "### Test 1\n" +
                "GET http://localhost:8080/api/test1\n" +
                "> {%\n" +
                "    client.test(\"Test 1 Result\", function() { client.assert(true); });\n" +
                "%}\n" +
                "\n" +
                "### Test 2\n" +
                "GET http://localhost:8080/api/test2\n" +
                "> {%\n" +
                "    client.test(\"Test 2 Result\", function() { client.assert(true); });\n" +
                "%}";
        
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

        String result = restTestService.runTestWithContent("Multi Block Custom Test", content);
        
        System.out.println("Result:\n" + result);
        assertTrue(result.contains("✅ Test 1 Result"), "Test 1 result should be present");
        assertTrue(result.contains("✅ Test 2 Result"), "Test 2 result should be present");
    }

    @Test
    public void testFullPostScriptFeatures() {
        String content = "GET http://localhost:8080/api/full\n" +
                "> {%\n" +
                "    client.test(\"Test Result\", function() { client.assert(true); });\n" +
                "    client.log(\"Hello from JS\");\n" +
                "    markdowner.heading(4, \"My Subheading\");\n" +
                "%}";
        
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

        String result = restTestService.runTestWithContent("Full Test", content);
        
        System.out.println("Result:\n" + result);
        assertTrue(result.contains("✅ Test Result"), "Test result should be present");
        assertTrue(result.contains("- Hello from JS"), "Log should be present");
        assertTrue(result.contains("#### My Subheading"), "Markdown heading should be present");
    }
}
