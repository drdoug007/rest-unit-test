# Rest Unit Test Framework

A Spring Boot-based framework for executing HTTP-based unit tests with integrated SQL verification and Markdown report generation.

## Features

- **Responsive Web Interface**: A full-height, three-column desktop UI to select, view, edit, and run tests.
- **Source View & Syntax Highlighting**: Integrated `.http` source code viewer with syntax highlighting (using highlight.js) for both source and reports.
- **Custom Browser-based Tests**: Create, edit, save, and delete custom `.http` tests directly in the browser using LocalStorage.
- **OpenAPI Import**: Automatically generate `.http` test files with basic assertions from OpenAPI 3 specifications (File, URL, or Paste).
- **Theme Support**: Automatic light/dark mode based on system settings, with forced light mode for PDF exports.
- **Sequential Execution**: Supports interleaved execution of SQL and JavaScript blocks before and after the HTTP request, preserving the order defined in the test file.
- **Robust Parser**: Strict case-sensitive HTTP method validation (GET, POST, etc.) prevents misidentification of test titles or scripts as requests.
- **SQL Verification**: Execute SQL queries against a database via SQL blocks or directly from JavaScript using `client.sqlQuery(sql)`.
- **JavaScript Assertions**: Write test logic in JavaScript using `< {% %}` blocks with `client.test()`, `client.assert()`, and `jsonPath()` support.
- **Variable Scoping**: JavaScript blocks are automatically wrapped in IIFEs to ensure local scoping and prevent variable collisions.
- **Variable Resolution**: Comprehensive support for `{{variable}}` resolution in URLs, headers, and bodies, including global and environment-specific variables.
- **In-place Variables**: Support for file-scoped variables using the `@name = value` syntax.
- **Dynamic Variables**: Built-in support for dynamic values like `{{$uuid}}`, `{{$timestamp}}`, `{{$randomInt}}`, etc.
- **Markdown Reports**: Automatically generates detailed reports in Markdown format with pretty-printed JSON and left-aligned table headers.
- **Custom Markdown Helper**: A `markdowner` helper available in JavaScript to add headings, tables, pretty-printed code blocks (including SQL), and raw Markdown content directly to reports.
- **Compatibility Aliases**: Supports `client.variables.global` as an alias for `client.global` for better compatibility with other REST clients.

## Getting Started

### Prerequisites

- Graalvm JDK 25 or higher (with `--enable-native-access=ALL-UNNAMED`)
- PostgreSQL (or your preferred database)

### Configuration

Sensitive database connection details should be stored in `src/main/resources/application-local.yaml`, which is ignored by version control. You can use `application.yaml` for default configurations and environment variable placeholders.

### Running Tests

#### Web Interface (Recommended)
Access the test runner UI at `http://localhost:8099/`. Here you can select tests from the sidebar, run them, view rendered markdown, and export to PDF.

#### API Endpoint
To run a test via API, use:
`GET /api/runtest/{testName}`

Example: `http://localhost:8099/api/runtest/cardealer`

## HTTP Test File Format

### JavaScript SQL Execution (Recommended)
You can execute SQL directly within JavaScript blocks:

```http
< {%
    const result = client.sqlQuery("SELECT * FROM my_table");
    markdowner.table(result.columns, result.data);
%}
```

### Legacy SQL Blocks
```http
# Pre-request blocks
# < SQL
### Query = "SELECT count(*) FROM my_table"
# SQL

< {%
    markdowner.heading(3, "Before Request");
    // Pre-request blocks are executed in order
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

    // Post-request blocks are executed in order
    markdowner.heading(3, "Response Details");

    // Compatibility alias for global variables
    client.variables.global.set("savedId", jsonPath(response.body, "$.id"));
%}
```

## Contributing

Please see [AGENTS.md](AGENTS.md) for details on our automated development process.
