# AI Agents in Development

This project uses AI agents (like Junie) to assist in development, refactoring, and documentation.

## Guidelines for Agents

- **Consistency**: Always maintain the existing code style and project structure.
- **Testing**: Before submitting any code change, ensure that existing tests pass and create reproduction tests for bugs.
- **Reporting**: Maintain the quality of the Markdown test reports, ensuring they are readable and correctly formatted.
- **Indentation**: Multi-line logs in Markdown reports must be indented by two spaces to maintain list structure.
- **Separation of Concerns**: Non-log markdown content (tables, headings) should be directed to the `markdownEntries` section via the `markdowner` helper.

## Agent Achievements

- Fixed Markdown table rendering in test reports.
- Introduced `markdown.js` helper for structured report generation.
- Refactored `RestTestService` to handle multi-line log indentation and dedicated markdown sections.
- Improved `.http` test file parsing for SQL and script blocks.
