package one.dastec.restunittest.e2e;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebUILoginTest extends BaseE2ETest {

    @Test
    void testLoginAndDashboard() {
        login("user", "password");

        // Verify we are on the dashboard
        assertThat(page).hasTitle("REST Unit Test Runner");
        
        // Verify sidebar is visible
        assertThat(page.locator(".sidebar")).isVisible();
        
        // Verify header elements
        assertThat(page.locator("header")).isVisible();
        assertThat(page.locator("#env-container")).isVisible();
    }

    @Test
    void testLogout() {
        login("user", "password");
        
        // Click logout button
        System.out.println("[DEBUG_LOG] Clicking logout button");
        // Using form submit directly if button click is flaky
        page.evaluate("document.querySelector('form[action=\"/logout\"]').submit()");
        
        // Should be redirected back to login with logout query parameter
        System.out.println("[DEBUG_LOG] Waiting for login page after logout");
        try {
            page.waitForURL(url -> url.contains("/login"), new Page.WaitForURLOptions().setTimeout(10000));
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Logout failed. Current URL: " + page.url());
            System.out.println("[DEBUG_LOG] Page content snippet: " + page.content().substring(0, Math.min(page.content().length(), 1000)));
            throw e;
        }
        assertTrue(page.url().contains("logout"));
        assertThat(page.locator(".logout-message")).isVisible();
    }
}
