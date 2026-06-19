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

public class DomMethodsReproductionTest {

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
    public void testDomMethodsInResponse() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        
        String xmlBody = "<slideshow><slide id=\"s1\">Text 1</slide><slide class=\"test\">Text 2</slide></slideshow>";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(xmlBody, headers, HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        String content = "GET http://localhost:8080/api/xml\n" +
                "\n" +
                "> {%\n" +
                "    client.test(\"getElementsByTagName\", function() {\n" +
                "        const slides = response.body.getElementsByTagName(\"slide\");\n" +
                "        client.assert(slides.length === 2, \"Should have 2 slides\");\n" +
                "        client.assert(slides[0].textContent === 'Text 1', \"First slide text mismatch\");\n" +
                "    });\n" +
                "    client.test(\"getElementById\", function() {\n" +
                "        const slide = response.body.getElementById(\"s1\");\n" +
                "        client.assert(slide !== null, \"Slide s1 not found\");\n" +
                "        client.assert(slide.tagName === 'slide', \"Tag name mismatch\");\n" +
                "    });\n" +
                "    client.test(\"getElementsByClassName\", function() {\n" +
                "        const slides = response.body.getElementsByClassName(\"test\");\n" +
                "        client.assert(slides.length === 1, \"Should have 1 slide with class test\");\n" +
                "        client.assert(slides[0].textContent === 'Text 2', \"Class slide text mismatch\");\n" +
                "    });\n" +
                "%}";

        String result = restTestService.runTestWithContent("DOM Methods Test", content);
        
        System.out.println("[DEBUG_LOG] Result:\n" + result);
        assertTrue(result.contains("✅ getElementsByTagName"), "getElementsByTagName failed: " + result);
        assertTrue(result.contains("✅ getElementById"));
        assertTrue(result.contains("✅ getElementsByClassName"));
    }

    @Test
    public void testDomParser() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        ResponseEntity<String> responseEntity = new ResponseEntity<>("<html></html>", headers, HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);

        String content = "GET http://localhost:8080/api/html\n" +
                "\n" +
                "> {%\n" +
                "    const xmlStr = '<q id=\"a\"><span class=\"test\" id=\"b\">hey!</span><span id=\"bar\">world</span><foo class=\"test\" id=\"x-foo\"/><bar name=\"x-foo-name\"/></q>';\n" +
                "    const doc = new DOMParser().parseFromString(xmlStr, \"application/xml\");\n" +
                "\n" +
                "    client.test(\"DOMParser getElementById\", function() {\n" +
                "        client.assert(doc.getElementById(\"a\") !== null, \"a not found\");\n" +
                "        client.assert(doc.getElementById(\"b\").textContent === 'hey!', \"b text mismatch\");\n" +
                "    });\n" +
                "    client.test(\"DOMParser getElementsByClassName\", function() {\n" +
                "        client.assert(doc.getElementsByClassName(\"test\").length === 2, \"test class count mismatch\");\n" +
                "    });\n" +
                "    client.test(\"DOMParser getElementsByTagName\", function() {\n" +
                "        client.assert(doc.getElementsByTagName(\"span\").length === 2, \"span tag count mismatch\");\n" +
                "    });\n" +
                "    client.test(\"DOMParser getElementsByName\", function() {\n" +
                "        client.assert(doc.getElementsByName(\"x-foo-name\").length === 1, \"name attribute search mismatch\");\n" +
                "    });\n" +
                "    client.test(\"DOMParser createElement\", function() {\n" +
                "        const newEl = doc.createElement(\"newtag\");\n" +
                "        client.assert(newEl.tagName === 'newtag', \"createElement mismatch\");\n" +
                "    });\n" +
                "%}";

        String result = restTestService.runTestWithContent("DOMParser Test", content);
        
        System.out.println(result);
        assertTrue(result.contains("✅ DOMParser getElementById"));
        assertTrue(result.contains("✅ DOMParser getElementsByClassName"));
        assertTrue(result.contains("✅ DOMParser getElementsByTagName"));
        assertTrue(result.contains("✅ DOMParser getElementsByName"));
        assertTrue(result.contains("✅ DOMParser createElement"));
    }
}
