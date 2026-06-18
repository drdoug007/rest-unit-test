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
        // No global context to close anymore
    }

    @Test
    public void testExecuteScript() {
        try (org.graalvm.polyglot.Context context = graalJsService.createContext()) {
            Value result = context.eval("js", "1 + 1");
            assertNotNull(result);
            assertEquals(2, result.asInt());
        }
    }
}
