package one.dastec.restunittest.services;

import one.dastec.restunittest.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DynamicVariablesReproductionTest {

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
        when(requestBodyUriSpec.body(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        
        restTestService = new RestTestService(dataSource, jdbcTemplate, builder, graalJsService, appProperties);
    }

    @Test
    public void testDynamicVariables() throws IOException {
        String content = new String(Files.readAllBytes(Paths.get("src/test/resources/httptestfiles/dynamic_variables.http")));
        
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"data\": \"{}\"}", HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        restTestService.runTestWithContent("Dynamic Variables Test", content);
        
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestBodyUriSpec).body(bodyCaptor.capture());
        
        String sentBody = bodyCaptor.getValue();
        System.out.println("[DEBUG_LOG] Sent Body:\n" + sentBody);
        
        assertFalse(sentBody.contains("{{$uuid}}"), "uuid should be resolved");
        assertFalse(sentBody.contains("{{$timestamp}}"), "timestamp should be resolved");
        assertFalse(sentBody.contains("{{$randomInt}}"), "randomInt should be resolved");
        assertFalse(sentBody.contains("{{$random.integer(100, 200)}}"), "random.integer should be resolved");
        assertFalse(sentBody.contains("{{$random.alphabetic(10)}}"), "random.alphabetic should be resolved");
    }
}
