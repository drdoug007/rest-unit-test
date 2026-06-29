package one.dastec.restunittest.e2e;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class DashboardVisibilityTest extends BaseE2ETest {

    @Test
    void testDashboardButtonVisibility() {
        page.navigate("http://localhost:" + port);
        
        // Login if needed
        if (page.url().contains("/login")) {
            page.fill("input[name='username']", "user");
            page.fill("input[name='password']", "password");
            page.click("button[type='submit']");
        }

        // Wait for page to load
        page.waitForSelector("header");

        // The dashboard button should be visible on desktop
        Locator dashboardBtn = page.locator("#dashboard-btn");
        assertThat(dashboardBtn).isVisible();
    }
}
