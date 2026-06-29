package one.dastec.restunittest.controllers;

import one.dastec.restunittest.entities.TestRun;
import one.dastec.restunittest.repositories.TestResultRepository;
import one.dastec.restunittest.repositories.TestRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class HistoryApiTest {

    private MockMvc mockMvc;
    private TestRunRepository testRunRepository;
    private TestResultRepository testResultRepository;

    @BeforeEach
    public void setup() {
        testRunRepository = Mockito.mock(TestRunRepository.class);
        testResultRepository = Mockito.mock(TestResultRepository.class);
        HistoryApi historyApi = new HistoryApi(testRunRepository, testResultRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(historyApi).build();
    }

    @Test
    public void testGetRecentRuns() throws Exception {
        TestRun run = new TestRun();
        run.setId(1L);
        run.setTestFileName("test.http");
        
        when(testRunRepository.findTop50ByOrderByExecutionTimeDesc()).thenReturn(List.of(run));

        mockMvc.perform(get("/api/history/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].testFileName").value("test.http"));
    }

    @Test
    public void testGetStats() throws Exception {
        when(testRunRepository.getSuccessRateByFile()).thenReturn(Collections.emptyList());
        when(testRunRepository.getDurationTrends()).thenReturn(Collections.emptyList());
        when(testResultRepository.getFailurePatterns()).thenReturn(Collections.emptyList());
        when(testResultRepository.getSlowestTests()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/history/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successRates").isArray())
                .andExpect(jsonPath("$.trends").isArray());
    }
}
