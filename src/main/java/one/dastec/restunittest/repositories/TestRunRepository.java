package one.dastec.restunittest.repositories;

import one.dastec.restunittest.entities.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {
    List<TestRun> findTop50ByOrderByExecutionTimeDesc();
    
    @Query("SELECT t.testFileName as name, COUNT(t) as count, SUM(t.passedTests) * 100.0 / SUM(t.totalTests) as successRate " +
           "FROM TestRun t GROUP BY t.testFileName")
    List<Map<String, Object>> getSuccessRateByFile();

    @Query("SELECT CAST(t.executionTime AS date) as date, AVG(t.durationMs) as avgDuration " +
           "FROM TestRun t GROUP BY CAST(t.executionTime AS date) ORDER BY CAST(t.executionTime AS date)")
    List<Map<String, Object>> getDurationTrends();
}
