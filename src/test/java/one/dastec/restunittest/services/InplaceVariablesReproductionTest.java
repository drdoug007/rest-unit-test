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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InplaceVariablesReproductionTest {

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
        graalJsService = new GraalJsService();
        restClient = Mockito.mock(RestClient.class);
        requestBodyUriSpec = Mockito.mock(RestClient.RequestBodyUriSpec.class);
        responseSpec = Mockito.mock(RestClient.ResponseSpec.class);
        appProperties = new AppProperties();
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
    public void testInplaceVariables() throws IOException {
        String content = new String(Files.readAllBytes(Paths.get("src/test/resources/httptestfiles/inplace_variables.http")));
        
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"data\": \"{}\"}", HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        restTestService.runTestWithContent("Inplace Variables Test", content);
        
        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestBodyUriSpec, atLeastOnce()).uri(uriCaptor.capture());
        
        List<String> uris = uriCaptor.getAllValues();
        System.out.println("[DEBUG_LOG] Captured URIs: " + uris);
        
        assertTrue(uris.get(0).contains("example.org:8080"), "URI should have resolved variables. Got: " + uris.get(0));
        assertTrue(uris.get(1).contains("example.org:8080"), "Second URI should have resolved variables. Got: " + uris.get(1));

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestBodyUriSpec).body(bodyCaptor.capture());
        String body = bodyCaptor.getValue();
        System.out.println("[DEBUG_LOG] Captured Body: " + body);
        assertTrue(body.contains("\"host\": \"example.org\""), "Body should have resolved host");
        assertTrue(body.contains("\"port\": \"8080\""), "Body should have resolved port");
    }
}
