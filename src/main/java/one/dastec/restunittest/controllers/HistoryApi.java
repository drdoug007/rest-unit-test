package one.dastec.restunittest.controllers;

import one.dastec.restunittest.entities.TestRun;
import one.dastec.restunittest.repositories.TestResultRepository;
import one.dastec.restunittest.repositories.TestRunRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class HistoryApi {
    private final TestRunRepository testRunRepository;
    private final TestResultRepository testResultRepository;

    public HistoryApi(TestRunRepository testRunRepository, TestResultRepository testResultRepository) {
        this.testRunRepository = testRunRepository;
        this.testResultRepository = testResultRepository;
    }

    @GetMapping("/recent")
    public List<TestRun> getRecentRuns() {
        return testRunRepository.findTop50ByOrderByExecutionTimeDesc();
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("successRates", testRunRepository.getSuccessRateByFile());
        stats.put("trends", testRunRepository.getDurationTrends());
        stats.put("failurePatterns", testResultRepository.getFailurePatterns());
        stats.put("slowestTests", testResultRepository.getSlowestTests());
        return stats;
    }
}
