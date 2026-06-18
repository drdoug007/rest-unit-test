# AI Agents in Development

This project uses AI agents (like Junie) to assist in development, refactoring, and documentation.

## Guidelines for Agents

- **Consistency**: Always maintain the existing code style and project structure.
- **Testing**: Before submitting any code change, ensure that existing tests pass and create reproduction tests for bugs.
- **Reporting**: Maintain the quality of the Markdown test reports, ensuring they are readable and correctly formatted.
- **Indentation**: Multi-line logs in Markdown reports must be indented by two spaces to maintain list structure.
- **Separation of Concerns**: Non-log markdown content (tables, headings) should be directed to the `markdownEntries` section via the `markdowner` helper.

## Agent Achievements

- Updated the web UI to occupy 80% width on desktop and added a PDF export button.
- Enhanced the test engine to support interleaved and ordered execution of SQL and JavaScript blocks before and after requests.
- Implemented IIFE wrapping for JavaScript blocks to ensure local scoping and prevent variable collisions.
- Formatted JSON response messages to be pretty-printed in the generated Markdown reports.
- Integrated `client.sqlQuery(sql)` to allow executing database queries directly from JavaScript.
- Ensured left-alignment of Markdown table column headers for better readability.
- Improved HTTP parser robustness to strictly validate methods and header formats, preventing misidentification of script lines.
- Fixed `TypeError` in `client.assert()` when handling null or undefined values from JavaScript.
- Enhanced variable resolution to correctly handle `{{baseUrl}}` even when not explicitly defined in properties.
- Configured JVM arguments to enable native access (`--enable-native-access=ALL-UNNAMED`) for both runtime and tests.
- Refactored `cardealer.http` to use correct database schema (column names and foreign key relations).
- Fixed `TypeError` in `cardealer.http` post-script by exposing `client.variables.global` as an alias for `client.global`.
- Fixed `URI with undefined scheme` error by enforcing case-sensitive HTTP method matching in the parser, preventing titles like "Get car" from being misidentified as requests.
- Updated documentation in `README.md` and `AGENTS.md` to reflect recent features, fixes, and architectural improvements.
- Moved sensitive datasource credentials to `application-local.yaml` and added it to `.gitignore`.
- Added a "View Source" button to the web UI to display the raw `.http` test file content.
- Improved the web UI by horizontally aligning action buttons using flex layout with consistent spacing.
- Optimized the web UI for desktop by introducing a three-column layout that displays the source code panel alongside the test report.
- Automatically fetch and display source code during test execution, removing the need for a separate "View Source" button on desktop.
- Integrated `highlight.js` to provide syntax highlighting for the `.http` source code display.
- Fixed syntax highlighting to ensure it is correctly applied to both the source code panel and Markdown report content.
- Improved syntax highlighting robustness by using `setTimeout` for deferred execution and adding defensive checks for the `hljs` library.
- Fixed syntax highlighting in Markdown reports to default to HTTP language for better visibility of headers and response data.
- Added a "Clone" button to the source panel to enable editing of `.http` test files.
- Implemented a "Run" capability for edited source code, allowing custom tests to be executed and reported instantly without saving to disk.
- Added a new backend API endpoint `POST /api/runtest/custom` to support execution of arbitrary `.http` content.
- Improved the web UI layout to be full-height on desktop, with scrollable sidebar, report, and source panels.
- Optimized the "Clone" editor to vertically fill the available space in the source panel.
- Fixed the source editor textarea to correctly fill all available vertical space on desktop.
- Implemented `LocalStorage` persistence for custom `.http` tests, allowing users to save, retrieve, update, and delete their own tests.
- Integrated custom tests into the sidebar with a management dropdown for renaming and deletion.
- Enhanced the test runner to seamlessly handle both server-side and browser-stored custom tests.
- Added a "Save" button to the source editor to allow persisting changes to custom `.http` tests without executing them.
- Updated the "Clone" button to be labeled "Edit" when viewing or editing custom `.http` tests.
- Added a "+ New" button to the sidebar to allow creating new custom `.http` test files directly in the browser.
- Implemented OpenAPI 3 (YAML/JSON) import capability to automatically generate `.http` test files with basic assertions.
- Ensured OpenAPI import robustly handles both YAML and JSON formats with case-insensitive file extension checks.
- Extended OpenAPI import to support fetching specifications from a URL and pasting from the clipboard.
- Added a "Run" button to the source panel to allow executing tests directly while viewing the code.
- Prevented automatic test execution when selecting a test from the sidebar.
- Ensured consistent button sizes across the web UI.
- Refactored `index.html` by moving CSS and JavaScript into external files (`styles.css` and `scripts.js`).
- Standardized font sizes across all buttons for better visual consistency.
- Implemented light and dark mode support based on platform system settings.
- Fixed an issue where the "+ New" and "Import OpenAPI" buttons were hidden in the sidebar.
- Configured PDF export to always use light mode regardless of the current system theme.
- Enhanced OpenAPI import to automatically generate example JSON request bodies from schemas.
- Fixed an issue where the text in the "Import OpenAPI" dropdown options was difficult to read in dark mode.
- Verified support for Basic, Digest, and Bearer authorization headers in `.http` files.
- Improved `client.global` mapping and variable propagation for better session state management in test scripts.
- Configured JVM arguments (`-XX:+EnableDynamicAgentLoading`, `-Xshare:off`, `--sun-misc-unsafe-memory-access=allow`) to suppress warnings related to Mockito agent loading, CDS, and terminally deprecated `sun.misc.Unsafe` usage during test execution and runtime.
- Enhanced OpenAPI import to generate `Authorization` headers based on security schemes.
- Improved OpenAPI import to correctly set `Content-Type` and `Accept` headers based on specification media types.
- Enhanced OpenAPI import to omit unnecessary headers and request bodies for `GET` and `DELETE` requests.
- Implemented Global Variables management for custom `.http` tests, including a popup dialog for key/value pairs.
- Updated the test runner to merge browser-defined global variables with server-side environment variables.
- Persisted custom global variables in `LocalStorage` linked to each custom test.
- Enhanced the Globals dialog to support multiple variables and fixed custom test execution to include global variables.
- Improved the Globals dialog by adding a styled table for variable management and refined the row addition/deletion logic.
- Fixed an issue where the "+ Add Variable" button was not displaying in the Globals modal.
- Integrated Globals management directly into the source panel for custom tests, enabling variable definition during creation, cloning, and editing.
- Implemented temporary state persistence for global variables in unsaved tests, ensuring they are correctly associated upon naming and saving.
- Fixed an issue where the "Save Globals" button was not displaying in the Globals modal.
- Enhanced test reports and server logs to include the full request details, including headers and body.
- Implemented automatic Base64 conversion for Basic Authentication headers when provided in "Basic username password" format.
- Enhanced the web UI to provide JavaScript syntax highlighting for pre-scripts and post-scripts within `.http` source files.
- Added support for JSON and SQL syntax highlighting in test reports and source views.
