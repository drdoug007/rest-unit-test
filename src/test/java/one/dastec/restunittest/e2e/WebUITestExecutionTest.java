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
        assertThat(page.locator(".cm-content")).containsText("GET");
        
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
        login("user", "password");
        
        // Wait for tests to load (indicates JS is initialized)
        page.waitForSelector("#test-list li:not(.loading)");
        
        // Click "+ New" button in sidebar
        page.click("#add-test-btn");
        
        String customTest = "### Playwright Custom Test\n" +
                "GET http://localhost:" + port + "/api/cardealer\n" +
                "Authorization: Basic dXNlcjpwYXNzd29yZA==\n\n" +
                "HTTP/1.1 200 OK\n" +
                "Content-Type: application/json\n\n" +
                "[\n" +
                "  {\"id\": 1, \"name\": \"Mock Dealer\"}\n" +
                "]\n\n" +
                "> {%\n" +
                "    client.test(\"Request executed successfully\", function() {\n" +
                "        client.assert(response.status === 200, \"Response status is not 200\");\n" +
                "    });\n" +
                "%}";
        
        // Wait for the editor container to be visible
        page.waitForSelector("#run-custom-btn");
        page.waitForSelector("#source-editor");

        // Use a more resilient way to set content into the CM6 editor
        page.evaluate("text => {\n" +
                "  if (window.editor) {\n" +
                "    window.editor.dispatch({\n" +
                "      changes: {from: 0, to: window.editor.state.doc.length, insert: text}\n" +
                "    });\n" +
                "  }\n" +
                "  // Fallback for runner\n" +
                "  window.currentSource = text;\n" +
                "}", customTest);
        
        // Click Run button for custom test
        page.click("#run-custom-btn");
        
        // Wait for report
        try {
            // Resilient wait for the specific local URL to appear in the report
            page.waitForCondition(() -> {
                String text = page.locator("#report-content").innerText();
                if (text.contains("example.com")) return false;
                return text.contains("Playwright Custom Test") || text.contains("http://localhost:" + port);
            }, new Page.WaitForConditionOptions().setTimeout(15000));
        } catch (Exception e) {
            String reportText = page.locator("#report-content").innerText();
            throw e;
        }
        
        // Verify results
        assertThat(page.locator("#report-content")).containsText("✅");
    }
}
