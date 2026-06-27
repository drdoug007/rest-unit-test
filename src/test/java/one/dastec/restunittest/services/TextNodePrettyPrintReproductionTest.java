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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class TextNodePrettyPrintReproductionTest {

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
    public void testPrettyPrintXmlTextNodes() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        
        String xmlBody = "<List><item><id>1</id><name>Luxury Motors</name></item></List>";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(xmlBody, headers, HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        String content = "GET http://localhost:8080/api/xml\n" +
                "\n" +
                "> {%\n" +
                "    client.test(\"Verify XML text nodes are on one line\", function() {\n" +
                "        const xml = response.body.xml;\n" +
                "        client.log(\"[DEBUG_LOG] XML:\\n\" + xml);\n" +
                "        client.assert(!xml.includes(\"<id>\\n\"), \"<id> should not be followed by newline\");\n" +
                "        client.assert(xml.includes(\"<id>1</id>\"), \"<id>1</id> should be on one line\");\n" +
                "    });\n" +
                "%}";

        String result = restTestService.runTestWithContent("Text Node Pretty Print Test", content);
        System.out.println("[DEBUG_LOG] Full Result:\n" + result);
        assertTrue(result.contains("✅ Verify XML text nodes are on one line"), "Expected text nodes to be on one line:\n" + result);
    }
}
