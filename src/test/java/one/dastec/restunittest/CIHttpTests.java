package one.dastec.restunittest;

import one.dastec.restunittest.services.RestTestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CIHttpTests {

    @Autowired
    private RestTestService restTestService;

    @LocalServerPort
    private int port;

    @Test
    public void runCiTests() {
        Map<String, Object> globals = new HashMap<>();
        globals.put("baseUrl", "http://localhost:" + port);
        
        String report = restTestService.runTestWithContent("ci-test", restTestService.getTestSource("ci-test"), globals);
        System.out.println("CI Test Report:\n" + report);
        
        assertTrue(report.contains("# Test Report: ci-test"), "Report title missing");
        assertFalse(report.contains("❌"), "HTTP Tests failed! See report:\n" + report);
        assertTrue(report.contains("✅ Status is 200"), "First test success marker missing");
    }
}
