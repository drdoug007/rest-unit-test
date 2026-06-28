package one.dastec.restunittest.e2e;

import org.junit.jupiter.api.Test;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * A simple demonstration test for Headed mode.
 * To run this in headed mode locally, use:
 * ./mvnw test "-Dtest=HeadedDemoTest" "-Dheadless=false"
 */
public class HeadedDemoTest extends BaseE2ETest {

    @Test
    void demoHeadedTest() {
        // 1. Navigate to login and log in
        login("user", "password");
        
        // 2. Verify we are on the dashboard
        assertThat(page).hasTitle("REST Unit Test Runner");
        
        // 3. Perform some visible actions
        page.click("#manage-envs-btn"); // Open environment management (gear icon)
        assertThat(page.locator("#env-modal")).isVisible();
        
        // Close modal
        page.click(".close-env-modal");
        assertThat(page.locator("#env-modal")).isHidden();
        
        // 4. Select a test from the sidebar
        page.waitForSelector("#test-list li:not(.loading)");
        page.click("li:has-text('cardealer')");
        
        // 5. Run the test
        page.click("#run-view-btn");
        
        // 6. Verify report appears
        assertThat(page.locator("#report-content")).containsText("Test Report");
        
        System.out.println("[DEBUG_LOG] Headed demo test completed successfully!");
    }
}
