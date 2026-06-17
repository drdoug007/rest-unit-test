package one.dastec.restunittest.controllers;

import one.dastec.restunittest.services.RestTestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
