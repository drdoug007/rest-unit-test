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

public class XmlContentTypeReproductionTest {

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
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        
        restTestService = new RestTestService(dataSource, jdbcTemplate, builder, graalJsService, appProperties);
    }

    @Test
    public void testXmlResponseContentType() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        
        String xmlBody = "<List><item><id>1</id><name>Luxury Motors</name></item></List>";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(xmlBody, headers, HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        String content = "GET http://localhost:8080/api/xml\n" +
                "\n" +
                "> {%\n" +
                "    client.test(\"Check XML Content-Type\", function() {\n" +
                "        client.assert(response.contentType.mimeType.contains(\"xml\"), \"Content-Type should contain xml\");\n" +
                "    });\n" +
                "%}";

        String result = restTestService.runTestWithContent("XML ContentType Test", content);
        
        System.out.println("[DEBUG_LOG] Result:\n" + result);
        assertTrue(result.contains("❌ Check XML Content-Type: TypeError"), "Should still fail on .mimeType.contains because it is a JS string");
        
        content = "GET http://localhost:8080/api/xml\n" +
                "\n" +
                "> {%\n" +
                "    client.test(\"Check XML Content-Type includes\", function() {\n" +
                "        client.assert(response.contentType.includes(\"xml\"), \"response.contentType.includes should work\");\n" +
                "        client.assert(response.contentType.contains(\"xml\"), \"response.contentType.contains should work\");\n" +
                "        client.assert(response.contentType == 'application/xml', \"response.contentType string comparison should work\");\n" +
                "    });\n" +
                "%}";
        result = restTestService.runTestWithContent("XML ContentType Test Helpers", content);
        System.out.println("[DEBUG_LOG] Result Helpers:\n" + result);
        assertTrue(result.contains("✅ Check XML Content-Type includes"), "Should pass Check XML Content-Type helpers");
    }
}
