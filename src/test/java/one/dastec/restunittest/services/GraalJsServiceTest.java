package one.dastec.restunittest.services;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GraalJsServiceTest {

    private GraalJsService graalJsService;

    @BeforeEach
    public void setUp() {
        graalJsService = new GraalJsService();
    }

    @AfterEach
    public void tearDown() {
        graalJsService.close();
    }

    @Test
    public void testExecuteScript() {
        Value result = graalJsService.executeScript("1 + 1");
        assertNotNull(result);
        assertEquals(2, result.asInt());
    }
}
