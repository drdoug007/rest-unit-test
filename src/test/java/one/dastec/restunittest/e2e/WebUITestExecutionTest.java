package one.dastec.restunittest.e2e;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class WebUITestExecutionTest extends BaseE2ETest {

    @Test
    void testRunExistingHttpTest() {
        login("user", "password");
        
        // Select 'cardealer' test from the sidebar
        // Note: The sidebar items have text like "cardealer HTTP" or "cardealer"
        page.click("text=cardealer");
        
        // Wait for source code to load
        assertThat(page.locator("#source-code")).containsText("GET");
        
        // Click the Run button (green one in the source panel)
        page.click("#run-view-btn");
        
        // Wait for report to appear
        page.waitForSelector("#report-content table, #report-content h2");
        
        // Verify report content contains success markers
        assertThat(page.locator("#report-content")).containsText("✅");
        assertThat(page.locator("#report-content")).containsText("Request executed successfully");
    }

    @Test
    void testCreateAndRunCustomTest() {
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));
        Assumptions.assumeFalse(headless, "Skipping CM6 editor test in headless mode");
        
        login("user", "password");
        
        // Click "+ New" button in sidebar
        page.click("#add-test-btn");
        
        String customTest = "### Playwright Custom Test\nGET http://localhost:" + port + "/api/cardealer";
        System.out.println("[DEBUG_LOG] Setting custom test content targeting port: " + port);
        
        // Wait for the editor container to be visible
        page.waitForSelector("#source-editor");

        // Use a more resilient way to set content into the CM6 editor
        page.evaluate("([text]) => {" +
                "  if (window.editor) {" +
                "    window.editor.dispatch({" +
                "      changes: {from: 0, to: window.editor.state.doc.length, insert: text}" +
                "    });" +
                "  }" +
                "  // Fallback for runner" +
                "  window.currentSource = text;" +
                "}", java.util.Collections.singletonList(customTest));
        
        // Click Run button for custom test
        page.click("#run-custom-btn");
        
        // Wait for report
        System.out.println("[DEBUG_LOG] Waiting for report content to update with port: " + port);
        
        try {
            // Resilient wait for the specific local URL to appear in the report
            page.waitForCondition(() -> {
                String text = page.locator("#report-content").innerText();
                if (text.contains("example.com")) return false;
                return text.contains("Playwright Custom Test") || text.contains("http://localhost:" + port);
            }, new Page.WaitForConditionOptions().setTimeout(15000));
        } catch (Exception e) {
            String reportText = page.locator("#report-content").innerText();
            System.out.println("[DEBUG_LOG] FAILED to find expected content in report. Current text: " + reportText);
            throw e;
        }
        
        // Verify results
        assertThat(page.locator("#report-content")).containsText("✅");
    }
}
