package one.dastec.restunittest.controllers;

import one.dastec.restunittest.models.CustomTestRequest;
import one.dastec.restunittest.services.RestTestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class RestTestApiTest {

    private RestTestApi restTestApi;
    private RestTestService restTestService;

    @BeforeEach
    void setUp() {
        restTestService = Mockito.mock(RestTestService.class);
        restTestApi = new RestTestApi(restTestService);
    }

    @Test
    void listTests_shouldReturnListOfHttpFiles() throws IOException {
        List<String> tests = restTestApi.listTests();
        assertNotNull(tests);
        // We know cardealer.http exists in src/main/resources/httptestfiles
        assertTrue(tests.contains("cardealer"));
    }
    @Test
    void runTestCustom_shouldCallServiceWithGlobals() {
        CustomTestRequest request = new CustomTestRequest();
        request.setName("My Custom Test");
        request.setContent("GET http://localhost:8080");
        Map<String, Object> globals = new HashMap<>();
        globals.put("token", "secret");
        request.setGlobals(globals);

        when(restTestService.runTestWithContent(eq("My Custom Test"), eq("GET http://localhost:8080"), anyMap()))
                .thenReturn("Report");

        String result = restTestApi.runTestCustom(request);

        assertEquals("Report", result);
        Mockito.verify(restTestService).runTestWithContent(eq("My Custom Test"), eq("GET http://localhost:8080"), eq(globals));
    }
}
