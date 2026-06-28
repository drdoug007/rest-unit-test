package one.dastec.restunittest.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.security.user.name=user",
        "spring.security.user.password=password"
})
public abstract class BaseE2ETest {
    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    @LocalServerPort
    protected int port;

    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));
        System.out.println("[DEBUG_LOG] Launching browser (headless=" + headless + ")");
        browser = playwright.chromium().launch(new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(headless));
    }

    @AfterAll
    static void closeBrowser() {
        playwright.close();
    }

    @BeforeEach
    void createContext() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 800));
        page = context.newPage();
        
        // Add console message listener
        page.onConsoleMessage(msg -> {
            System.out.println("[BROWSER_CONSOLE] [" + msg.type() + "] " + msg.text());
        });
        
        page.onResponse(response -> {
            if (response.status() >= 400) {
                System.out.println("[BROWSER_RESPONSE_ERROR] " + response.status() + " " + response.url());
            }
        });
        
        page.onRequestFailed(request -> {
            System.out.println("[BROWSER_REQUEST_FAILED] " + request.url() + " " + request.failure());
        });
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    protected void login(String username, String password) {
        String loginUrl = getBaseUrl() + "/login.html";
        System.out.println("[DEBUG_LOG] Navigating to: " + loginUrl);
        page.navigate(loginUrl);
        System.out.println("[DEBUG_LOG] Page title: " + page.title());
        page.fill("input[name='username']", username);
        page.fill("input[name='password']", password);
        
        System.out.println("[DEBUG_LOG] Clicking Sign In button");
        page.click("button[type='submit']");
        
        System.out.println("[DEBUG_LOG] Current URL after submit click: " + page.url());
        
        // Wait for potential login failure message or successful redirection
        try {
            // Wait for either the dashboard element or the error message to appear
            page.waitForSelector("#test-list, .error-message", new Page.WaitForSelectorOptions().setTimeout(10000));
            
            if (page.locator(".error-message").isVisible()) {
                String error = page.locator(".error-message").innerText();
                System.out.println("[DEBUG_LOG] Login failed with error: " + error);
                throw new RuntimeException("Login failed: " + error);
            }
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Login process failed. Page content snippet: " + page.content().substring(0, Math.min(page.content().length(), 1000)));
            throw e;
        }
        
        System.out.println("[DEBUG_LOG] Current URL after #test-list appeared: " + page.url());
    }
}
