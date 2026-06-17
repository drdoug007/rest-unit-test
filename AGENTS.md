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
