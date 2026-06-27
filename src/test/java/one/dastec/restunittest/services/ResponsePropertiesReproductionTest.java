package one.dastec.restunittest.services;

import one.dastec.restunittest.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class ResponsePropertiesReproductionTest {

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
    public void testResponseProperties() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Custom-Header", "CustomValue");
        
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"key\": \"value\", \"id\": 123}", headers, HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        String content = "GET http://localhost:8080/api/test\n" +
                "\n" +
                "> {%\n" +
                "    client.test(\"Status property\", function() {\n" +
                "        client.assert(response.status === 200, \"Status should be 200\");\n" +
                "    });\n" +
                "    client.test(\"Headers property\", function() {\n" +
                "        client.assert(response.headers['X-Custom-Header'] === 'CustomValue', \"Custom header missing\");\n" +
                "    });\n" +
                "    client.test(\"ContentType property\", function() {\n" +
                "        client.assert(response.contentType.mimeType === 'application/json', \"MimeType should be application/json\");\n" +
                "    });\n" +
                "    client.test(\"Body property as object\", function() {\n" +
                "        client.assert(typeof response.body === 'object', \"Body should be an object\");\n" +
                "        client.assert(response.body.key === 'value', \"Body key missing\");\n" +
                "        client.assert(response.body.id === 123, \"Body id missing\");\n" +
                "    });\n" +
                "%}";

        String result = restTestService.runTestWithContent("Response Properties Test", content);
        
        System.out.println(result);
        assertTrue(result.contains("✅ Status property"));
        assertTrue(result.contains("✅ Headers property"));
        assertTrue(result.contains("✅ ContentType property"));
        assertTrue(result.contains("✅ Body property as object"));
    }

    @Test
    public void testResponsePropertiesText() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        
        ResponseEntity<String> responseEntity = new ResponseEntity<>("Hello World", headers, HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        String content = "GET http://localhost:8080/api/test\n" +
                "\n" +
                "> {%\n" +
                "    client.test(\"Body property as string\", function() {\n" +
                "        client.assert(typeof response.body === 'string', \"Body should be a string\");\n" +
                "        client.assert(response.body === 'Hello World', \"Body content mismatch\");\n" +
                "    });\n" +
                "%}";

        String result = restTestService.runTestWithContent("Response Properties Text Test", content);
        
        System.out.println(result);
        assertTrue(result.contains("✅ Body property as string"));
    }
}
