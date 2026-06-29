package one.dastec.restunittest.repositories;

import one.dastec.restunittest.entities.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {
    @Query("SELECT t.errorMessage as error, COUNT(t) as count " +
           "FROM TestResult t WHERE t.status = 'FAILURE' GROUP BY t.errorMessage ORDER BY COUNT(t) DESC")
    List<Map<String, Object>> getFailurePatterns();

    @Query("SELECT t.testName as name, AVG(t.responseTimeMs) as avgResponseTime " +
           "FROM TestResult t GROUP BY t.testName ORDER BY AVG(t.responseTimeMs) DESC")
    List<Map<String, Object>> getSlowestTests();
}
