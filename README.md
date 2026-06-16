# Rest Unit Test Framework

A Spring Boot-based framework for executing HTTP-based unit tests with integrated SQL verification and Markdown report generation.

## Features

- **Web Interface**: A responsive web-based UI to select, run, and display test reports with live rendering.
- **PDF Export**: Export generated test reports to PDF directly from the web interface.
- **Sequential Execution**: Supports SQL blocks and a consolidated JavaScript block before and after the HTTP request.
- **HTTP Request Execution**: Supports standard HTTP methods (GET, POST, etc.) with variable resolution.
- **SQL Verification**: Execute SQL queries against a database to verify state changes or fetch data for assertions.
- **JavaScript Assertions**: Use GraalJS to write powerful test logic and assertions in JavaScript.
- **Markdown Reports**: Automatically generates detailed test reports in Markdown format with pretty-printed JSON.
- **Custom Markdown Helper**: A `markdowner` helper available in JavaScript to add headings, tables, and code blocks directly to reports.

## Getting Started

### Prerequisites

- Graalvm JDK 25 or higher
- PostgreSQL (or your preferred database)

### Configuration

Update `src/main/resources/application.yaml` with your database connection details. These properties are also exposed as global variables (e.g., `environment`, `app`) in the JavaScript test context.

### Running Tests

#### Web Interface (Recommended)
Access the test runner UI at `http://localhost:8099/`. Here you can select tests from the sidebar, run them, view rendered markdown, and export to PDF.

#### API Endpoint
To run a test via API, use:
`GET /api/runtest/{testName}`

Example: `http://localhost:8099/api/runtest/cardealer`

## HTTP Test File Format

```http
# Pre-request blocks
# < SQL
### Query = "SELECT count(*) FROM my_table"
# SQL

< {%
    markdowner.heading(3, "Before Request");
    // All < {% blocks are consolidated into one pre-script execution
    const count = client.global.get("count");
%}

### Test Description
GET http://example.com/api/resource
Content-Type: application/json

{
  "key": "value"
}

# Post-request blocks
> {%
    client.test("Status is 200", function() {
        client.assert(response.status === 200, "Response status is not 200");
    });

    // All > {% blocks are consolidated into one post-script execution
    markdowner.heading(3, "Response Details");
%}
```

## Contributing

Please see [AGENTS.md](AGENTS.md) for details on our automated development process.
