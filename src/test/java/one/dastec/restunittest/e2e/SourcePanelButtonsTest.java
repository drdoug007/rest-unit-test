package one.dastec.restunittest.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class SourcePanelButtonsTest extends BaseE2ETest {

    @Test
    void testSourcePanelButtonsVisibility() {
        page.navigate("http://localhost:" + port);
        
        // Login if needed
        if (page.url().contains("/login")) {
            page.fill("input[name='username']", "user");
            page.fill("input[name='password']", "password");
            page.click("button[type='submit']");
        }

        // 1. Check server-side test buttons
        page.waitForSelector("#test-list li:not(.loading)");
        Locator serverTest = page.locator("#test-list li").filter(new Locator.FilterOptions().setHasText("cardealer")).first();
        serverTest.click();

        // Verify "Clone" button
        Locator cloneBtn = page.locator("#clone-btn");
        assertThat(cloneBtn).isVisible();
        assertThat(cloneBtn).hasText("Clone");

        Locator runViewBtn = page.locator("#run-view-btn");
        assertThat(runViewBtn).isVisible();

        // 2. Check custom test buttons
        page.click("#add-test-btn");
        
        // After clicking "+ New", it should show Save, Run, Globals and Cancel
        Locator runCustomBtn = page.locator("#run-custom-btn");
        assertThat(runCustomBtn).isVisible();
        
        // Now select an existing custom test
        page.onDialog(dialog -> dialog.accept("MyCustomTest"));
        page.click("#save-custom-btn");
        
        // Wait for it to appear in list and click it
        page.waitForSelector("#test-list li:has-text('MyCustomTest')");
        page.click("#test-list li:has-text('MyCustomTest')");
        
        // Check if Run button is visible for selected custom test
        assertThat(page.locator("#run-custom-btn")).isVisible();
    }

    @Test
    void testCustomTestButtonsBehavior() {
        page.navigate("http://localhost:" + port);

        // Login if needed
        if (page.url().contains("/login")) {
            page.fill("input[name='username']", "user");
            page.fill("input[name='password']", "password");
            page.click("button[type='submit']");
        }

        // 1. Create a custom test
        page.click("#add-test-btn");
        page.onDialog(dialog -> dialog.accept("BehaviorTest"));
        page.click("#save-custom-btn");

        // 2. Select it
        page.waitForSelector("#test-list li:has-text('BehaviorTest')");
        page.click("#test-list li:has-text('BehaviorTest')");

        // 3. Test Globals (Environment) button
        Locator globalsBtn = page.locator("#globals-btn");
        assertThat(globalsBtn).isVisible();
        globalsBtn.click();
        
        // Modal should appear
        assertThat(page.locator("#globals-modal")).isVisible();
        page.click(".close-modal");
        assertThat(page.locator("#globals-modal")).isHidden();

        // 4. Test Environment button (Gear icon in header)
        Locator envBtn = page.locator("#manage-envs-btn");
        assertThat(envBtn).isVisible();
        envBtn.click();
        assertThat(page.locator("#env-modal")).isVisible();
        page.click(".close-env-modal");
        assertThat(page.locator("#env-modal")).isHidden();

        // 5. Test Cancel button
        // When a custom test is selected, the "Clone" button should be "Cancel"
        Locator cancelBtn = page.locator("#clone-btn");
        assertThat(cancelBtn).hasText("Cancel");
        
        // Type something in editor
        page.evaluate("window.editor.dispatch({changes: {from: 0, insert: 'modified content'}})");
        
        // Click Cancel
        cancelBtn.click();
        
        // It should revert or at least not "do nothing". 
        // Based on the issue, it "does nothing".
        // If it works, it should probably stay in custom test view but maybe reset isEditing or similar.
        // Actually if it's already a custom test, Cancel should probably just reset the editor to the last saved state.
        
        // Verify it doesn't do nothing - e.g. it should stay visible and maybe the text should still be "Cancel" or switch to "Edit"?
        // Wait, if it's a custom test, it's ALWAYS in editor mode now (recent achievement).
        // So "Cancel" should revert changes.
        
        String content = (String) page.evaluate("window.editor.state.doc.toString()");
        assertNotEquals("modified content", content);
    }

    @Test
    void testCloneButton() {
        page.navigate("http://localhost:" + port);

        // Login if needed
        if (page.url().contains("/login")) {
            page.fill("input[name='username']", "user");
            page.fill("input[name='password']", "password");
            page.click("button[type='submit']");
        }

        page.waitForSelector("#test-list li:not(.loading)");
        Locator serverTest = page.locator("#test-list li").filter(new Locator.FilterOptions().setHasText("cardealer")).first();
        serverTest.click();

        Locator cloneBtn = page.locator("#clone-btn");
        assertThat(cloneBtn).hasText("Clone");
        
        // Click Clone
        cloneBtn.click();
        
        // Should switch to editing mode
        assertThat(cloneBtn).hasText("Cancel");
        assertThat(page.locator("#source-editor")).isVisible();
        assertThat(page.locator("#run-custom-btn")).isVisible();
        
        // Click Cancel
        cloneBtn.click();
        assertThat(cloneBtn).hasText("Clone");
        assertThat(page.locator("#source-content")).isVisible();
    }

    @Test
    void testCancelUnsavedTest() {
        page.navigate("http://localhost:" + port);

        // Login if needed
        if (page.url().contains("/login")) {
            page.fill("input[name='username']", "user");
            page.fill("input[name='password']", "password");
            page.click("button[type='submit']");
        }

        page.click("#add-test-btn");
        assertThat(page.locator("#clone-btn")).hasText("Cancel");
        assertThat(page.locator("#source-editor")).isVisible();

        // Click Cancel on unsaved new test
        page.click("#clone-btn");

        // Should go back to initial state
        assertThat(page.locator("#source-editor")).isHidden();
        assertThat(page.locator("#run-custom-btn")).isHidden();
    }

    @Test
    void testQuickAddEnvButton() {
        page.navigate("http://localhost:" + port);

        // Login if needed
        if (page.url().contains("/login")) {
            page.fill("input[name='username']", "user");
            page.fill("input[name='password']", "password");
            page.click("button[type='submit']");
        }

        // Test Quick Add Environment (+) button
        Locator addEnvQuickBtn = page.locator("#add-env-quick-btn");
        assertThat(addEnvQuickBtn).isVisible();
        addEnvQuickBtn.click();
        
        // Modal should appear
        assertThat(page.locator("#env-modal")).isVisible();
        // Should have a new environment selected or being edited
        assertThat(page.locator("#env-edit-panel")).isVisible();
        
        page.click(".close-env-modal");
        assertThat(page.locator("#env-modal")).isHidden();
    }
}
