package one.dastec.restunittest.e2e;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaScriptDebuggerTest extends BaseE2ETest {

    @Test
    void testDebuggerStepThrough() {
        login("user", "password");

        // Wait for tests to load
        page.waitForSelector("#test-list li:not(.loading)");

        // Click "+ New" button in sidebar
        page.click("#add-test-btn");

        String customTest = "### Debugger Test\n" +
                "GET http://localhost:" + port + "/api/cardealer\n\n" +
                "> {%\n" +
                "    console.log('Line 1');\n" +
                "    console.log('Line 2');\n" +
                "    client.test(\"Debug Success\", function() {\n" +
                "        client.assert(200 === 200);\n" +
                "    });\n" +
                "%}";

        // Set editor content
        page.evaluate("text => {\n" +
                "  if (window.editor) {\n" +
                "    window.editor.dispatch({\n" +
                "      changes: {from: 0, to: window.editor.state.doc.length, insert: text}\n" +
                "    });\n" +
                "  }\n" +
                "}", customTest);

        // Wait for editor to update
        page.waitForTimeout(500);

        // Set breakpoint on Line 5 (console.log('Line 1')) using helper
        page.evaluate("line => { if (window.toggleBreakpointAtLine) window.toggleBreakpointAtLine(line); }", 5);

        // Click Debug button
        page.click("#debug-custom-btn");

        // Wait for debugger controls to appear
        page.waitForSelector("#debugger-controls:visible");

        // Verify that we are paused (UI should show controls)
        assertThat(page.locator("#debugger-controls")).isVisible();

        // Verify execution point is highlighted
        assertThat(page.locator(".cm-debug-line")).isVisible();
        assertThat(page.locator(".debug-marker")).isVisible();

        // Click Resume
        page.click("#debug-resume-btn");

        // Wait for report to appear
        page.waitForSelector("#report-content table, #report-content h2");

        // Verify results
        assertThat(page.locator("#report-content")).containsText("✅");
        assertThat(page.locator("#report-content")).containsText("Debug Success");
        
        // Debugger controls should be hidden after execution finishes
        assertThat(page.locator("#debugger-controls")).isHidden();
    }

    @Test
    void testDebuggerWithBlankLines() {
        login("user", "password");

        // Wait for tests to load
        page.waitForSelector("#test-list li:not(.loading)");

        // Click "+ New" button in sidebar
        page.click("#add-test-btn");

        String customTest = "### Debugger Test Blank Lines\n" +
                "GET http://localhost:" + port + "/api/cardealer\n\n" +
                "> {%\n" +
                "\n" + // Line 5
                "    console.log('Line after blank');\n" + // Line 6
                "%}";

        // Set editor content
        page.evaluate("text => {\n" +
                "  if (window.editor) {\n" +
                "    window.editor.dispatch({\n" +
                "      changes: {from: 0, to: window.editor.state.doc.length, insert: text}\n" +
                "    });\n" +
                "  }\n" +
                "}", customTest);

        // Wait for editor to update
        page.waitForTimeout(500);

        // Set breakpoint on Line 6 (console.log)
        page.evaluate("line => { if (window.toggleBreakpointAtLine) window.toggleBreakpointAtLine(line); }", 6);

        // Click Debug button
        page.click("#debug-custom-btn");

        // Wait for debugger controls to appear
        page.waitForSelector("#debugger-controls:visible");

        // Check which line is highlighted
        Boolean isLine6Highlighted = (Boolean) page.evaluate("() => {\n" +
                "  const el = document.querySelector('.cm-debug-line');\n" +
                "  if (!el) return false;\n" +
                "  // posAtDOM might return position, we need line number\n" +
                "  const pos = window.editor.posAtDOM(el);\n" +
                "  const line = window.editor.state.doc.lineAt(pos).number;\n" +
                "  console.log('Paused at line:', line);\n" +
                "  return line === 6;\n" +
                "}");

        assertTrue(isLine6Highlighted, "Line 6 should be highlighted, but it might be Line 5 due to trimming bug");

        // Click Resume
        page.click("#debug-resume-btn");
        page.waitForSelector("#report-content table, #report-content h2");
    }

    @Test
    void testDebuggerWithMultiplePostBlocks() {
        login("user", "password");

        // Wait for tests to load
        page.waitForSelector("#test-list li:not(.loading)");

        // Click "+ New" button in sidebar
        page.click("#add-test-btn");

        String customTest = "### Multiple Post Blocks\n" +
                "GET http://localhost:" + port + "/api/cardealer\n\n" +
                "> {%\n" +
                "    console.log('Block 1');\n" + // Line 5
                "%}\n\n" +
                "> {%\n" +
                "    console.log('Block 2');\n" + // Line 9
                "%}";

        // Set editor content
        page.evaluate("text => {\n" +
                "  if (window.editor) {\n" +
                "    window.editor.dispatch({\n" +
                "      changes: {from: 0, to: window.editor.state.doc.length, insert: text}\n" +
                "    });\n" +
                "  }\n" +
                "}", customTest);

        // Wait for editor to update
        page.waitForTimeout(500);

        // Set breakpoints on Line 5 and Line 9
        page.evaluate("line => { if (window.toggleBreakpointAtLine) window.toggleBreakpointAtLine(line); }", 5);
        page.evaluate("line => { if (window.toggleBreakpointAtLine) window.toggleBreakpointAtLine(line); }", 9);

        // Click Debug button
        page.click("#debug-custom-btn");

        // Wait for debugger controls to appear (paused at line 5)
        page.waitForSelector("#debugger-controls:visible");

        // Check which line is highlighted
        Integer pausedLine1 = (Integer) page.evaluate("() => {\n" +
                "  const el = document.querySelector('.cm-debug-line');\n" +
                "  if (!el) return null;\n" +
                "  const pos = window.editor.posAtDOM(el);\n" +
                "  return window.editor.state.doc.lineAt(pos).number;\n" +
                "}");
        assertEquals(5, pausedLine1, "Should be paused at line 5");

        // Click Resume
        page.click("#debug-resume-btn");

        // Wait to pause again at line 9
        // We need to wait for the UI to update the highlight
        page.waitForFunction("() => {\n" +
                "  const el = document.querySelector('.cm-debug-line');\n" +
                "  if (!el) return false;\n" +
                "  const pos = window.editor.posAtDOM(el);\n" +
                "  return window.editor.state.doc.lineAt(pos).number === 9;\n" +
                "}", null, new Page.WaitForFunctionOptions().setTimeout(5000));

        Integer pausedLine2 = (Integer) page.evaluate("() => {\n" +
                "  const el = document.querySelector('.cm-debug-line');\n" +
                "  if (!el) return null;\n" +
                "  const pos = window.editor.posAtDOM(el);\n" +
                "  return window.editor.state.doc.lineAt(pos).number;\n" +
                "}");
        assertEquals(9, pausedLine2, "Should be paused at line 9");

        // Click Resume
        page.click("#debug-resume-btn");
        page.waitForSelector("#report-content table, #report-content h2");
    }

    @Test
    void testDebuggerVariables() {
        login("user", "password");

        // Wait for tests to load
        page.waitForSelector("#test-list li:not(.loading)");

        // Click "+ New" button in sidebar
        page.click("#add-test-btn");

        String customTest = "### Variables Test\n" +
                "GET http://localhost:" + port + "/api/cardealer\n\n" +
                "> {%\n" +
                "    var localX = 42;\n" +
                "    var localY = 'Hello World';\n" +
                "    console.log(localX);\n" + // Line 7
                "%}";

        // Set editor content
        page.evaluate("text => {\n" +
                "  if (window.editor) {\n" +
                "    window.editor.dispatch({\n" +
                "      changes: {from: 0, to: window.editor.state.doc.length, insert: text}\n" +
                "    });\n" +
                "  }\n" +
                "}", customTest);

        // Wait for editor to update
        page.waitForTimeout(500);

        // Set breakpoint on Line 7
        page.evaluate("line => { if (window.toggleBreakpointAtLine) window.toggleBreakpointAtLine(line); }", 7);

        // Click Debug button
        page.click("#debug-custom-btn");

        // Wait for debugger controls and variables panel
        page.waitForSelector("#debugger-variables:visible");

        // Verify that variables are displayed
        assertThat(page.locator("#variables-list")).containsText("localX:42");
        assertThat(page.locator("#variables-list")).containsText("localY:Hello World");
        
        // Click Resume
        page.click("#debug-resume-btn");
        page.waitForSelector("#report-content table, #report-content h2");
    }

    @Test
    void testDebuggerResultSetDisplay() {
        login("user", "password");

        // Wait for tests to load
        page.waitForSelector("#test-list li:not(.loading)");

        // Click "+ New" button in sidebar
        page.click("#add-test-btn");

        String customTest = "### SQL Debugger Test\n" +
                "GET http://localhost:" + port + "/api/cardealer\n\n" +
                "> {%\n" +
                "    const result = client.sqlQuery(\"SELECT 1 as id, 'Ford' as make UNION SELECT 2 as id, 'Tesla' as make\");\n" +
                "    const resultSet = result.data;\n" +
                "    console.log('Paused here');\n" +
                "%}";

        // Set editor content
        page.evaluate("text => {\n" +
                "  if (window.editor) {\n" +
                "    window.editor.dispatch({\n" +
                "      changes: {from: 0, to: window.editor.state.doc.length, insert: text}\n" +
                "    });\n" +
                "  }\n" +
                "}", customTest);

        // Wait for editor to update
        page.waitForTimeout(500);

        // Set breakpoint on Line 7 (console.log)
        page.evaluate("line => { if (window.toggleBreakpointAtLine) window.toggleBreakpointAtLine(line); }", 7);

        // Click Debug button
        page.click("#debug-custom-btn");

        // Wait for debugger controls to appear
        page.waitForSelector("#debugger-controls:visible");

        // Check variables panel
        page.waitForSelector("#variables-list:visible");
        
        String variablesText = page.textContent("#variables-list");
        
        // It should NOT contain "JavaObject"
        assertTrue(!variablesText.contains("JavaObject"), "Variables panel should not contain JavaObject string: " + variablesText);
        
        // It should contain the values from the SQL query
        // The exact formatting depends on my formatDebugValue implementation.
        // It should look something like: resultSet: [{ID: 1, MAKE: "Ford"}, {ID: 2, MAKE: "Tesla"}]
        assertTrue(variablesText.contains("Ford"), "Variables panel should contain 'Ford': " + variablesText);
        assertTrue(variablesText.contains("Tesla"), "Variables panel should contain 'Tesla': " + variablesText);
        assertTrue(variablesText.contains("resultSet"), "Variables panel should contain 'resultSet': " + variablesText);

        // Click Resume
        page.click("#debug-resume-btn");
        page.waitForSelector("#report-content table, #report-content h2");
    }

    @Test
    void testDebuggerStepInto() {
        login("user", "password");

        // Wait for tests to load
        page.waitForSelector("#test-list li:not(.loading)");

        // Click "+ New" button in sidebar
        page.click("#add-test-btn");

        String customTest = "### Step Into Test\n" +
                "GET http://localhost:" + port + "/api/cardealer\n\n" +
                "> {%\n" +
                "    function myFunc() {\n" + // Line 5
                "        console.log('Inside func');\n" + // Line 6
                "    }\n" +
                "    myFunc();\n" + // Line 8
                "    console.log('Done');\n" + // Line 9
                "%}";

        // Set editor content
        page.evaluate("text => {\n" +
                "  if (window.editor) {\n" +
                "    window.editor.dispatch({\n" +
                "      changes: {from: 0, to: window.editor.state.doc.length, insert: text}\n" +
                "    });\n" +
                "  }\n" +
                "}", customTest);

        // Wait for editor to update
        page.waitForTimeout(500);

        // Set breakpoint on Line 8
        page.evaluate("line => { if (window.toggleBreakpointAtLine) window.toggleBreakpointAtLine(line); }", 8);

        // Click Debug button
        page.click("#debug-custom-btn");

        // Wait for debugger controls to appear
        page.waitForSelector("#debugger-controls:visible");

        // Verify paused at Line 8
        Integer pausedLine1 = (Integer) page.evaluate("() => {\n" +
                "  const el = document.querySelector('.cm-debug-line');\n" +
                "  if (!el) return null;\n" +
                "  const pos = window.editor.posAtDOM(el);\n" +
                "  return window.editor.state.doc.lineAt(pos).number;\n" +
                "}");
        assertEquals(8, pausedLine1, "Should be paused at line 8");

        // Click Step Into
        page.click("#debug-step-into-btn");

        // Wait for pause to update
        page.waitForTimeout(1000);

        // Verify paused at Line 6 (inside function)
        Integer pausedLine2 = (Integer) page.evaluate("() => {\n" +
                "  const el = document.querySelector('.cm-debug-line');\n" +
                "  if (!el) return null;\n" +
                "  const pos = window.editor.posAtDOM(el);\n" +
                "  return window.editor.state.doc.lineAt(pos).number;\n" +
                "}");
        assertEquals(6, pausedLine2, "Should be paused at line 6 after step into");

        // Click Step Over (should go to end of function or next line after call?)
        // In GraalJS, step over from inside function should go to next line in function if any, or return to caller.
        page.click("#debug-step-over-btn");
        page.waitForTimeout(1000);

        // Wait for pause to update to line 9 (after call)
        Integer pausedLine3 = (Integer) page.evaluate("() => {\n" +
                "  const el = document.querySelector('.cm-debug-line');\n" +
                "  if (!el) return null;\n" +
                "  const pos = window.editor.posAtDOM(el);\n" +
                "  return window.editor.state.doc.lineAt(pos).number;\n" +
                "}");
        assertEquals(9, pausedLine3, "Should be paused at line 9 after stepping out of function");

        // Click Resume
        page.click("#debug-resume-btn");
        page.waitForSelector("#report-content table, #report-content h2");
    }

    @Test
    void testDebuggerCrossBlockStepping() {
        login("user", "password");

        // Wait for tests to load
        page.waitForSelector("#test-list li:not(.loading)");

        // Click "+ New" button in sidebar
        page.click("#add-test-btn");

        String customTest = "### Block 1\n" +
                "GET http://localhost:" + port + "/api/cardealer\n\n" +
                "> {%\n" +
                "    console.log('Block 1');\n" + // Line 5
                "%}\n\n" +
                "### Block 2\n" +
                "GET http://localhost:" + port + "/api/cardealer\n\n" +
                "> {%\n" +
                "    console.log('Block 2');\n" + // Line 11
                "%}";

        // Set editor content
        page.evaluate("text => {\n" +
                "  if (window.editor) {\n" +
                "    window.editor.dispatch({\n" +
                "      changes: {from: 0, to: window.editor.state.doc.length, insert: text}\n" +
                "    });\n" +
                "  }\n" +
                "}", customTest);

        // Wait for editor to update
        page.waitForTimeout(500);

        // Set breakpoint on Line 5
        page.evaluate("line => { if (window.toggleBreakpointAtLine) window.toggleBreakpointAtLine(line); }", 5);

        // Click Debug button
        page.click("#debug-custom-btn");

        // Wait for debugger controls to appear
        page.waitForSelector("#debugger-controls:visible");

        // Verify paused at Line 5
        Integer pausedLine1 = (Integer) page.evaluate("() => {\n" +
                "  const el = document.querySelector('.cm-debug-line');\n" +
                "  if (!el) return null;\n" +
                "  const pos = window.editor.posAtDOM(el);\n" +
                "  return window.editor.state.doc.lineAt(pos).number;\n" +
                "}");
        assertEquals(5, pausedLine1, "Should be paused at line 5");

        // Click Step Over
        page.click("#debug-step-over-btn");

        // Wait for pause to update to Line 12
        // It might take some time because the request in between needs to finish
        page.waitForFunction("() => {\n" +
                "  const el = document.querySelector('.cm-debug-line');\n" +
                "  if (!el) return false;\n" +
                "  const pos = window.editor.posAtDOM(el);\n" +
                "  return window.editor.state.doc.lineAt(pos).number === 12;\n" +
                "}", null, new Page.WaitForFunctionOptions().setTimeout(5000));

        Integer pausedLine2 = (Integer) page.evaluate("() => {\n" +
                "  const el = document.querySelector('.cm-debug-line');\n" +
                "  const pos = window.editor.posAtDOM(el);\n" +
                "  return window.editor.state.doc.lineAt(pos).number;\n" +
                "}");
        assertEquals(12, pausedLine2, "Should be paused at line 12 after stepping over Block 1");

        // Click Resume
        page.click("#debug-resume-btn");
        page.waitForSelector("#report-content table, #report-content h2");
    }
}
