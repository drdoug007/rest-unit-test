# Rest Unit Test Framework

A Spring Boot-based framework for executing HTTP-based unit tests with integrated SQL verification and Markdown report generation.

## Features

- **Environment Management**: Define and switch between different sets of variables (e.g., Development, Staging, Production) via the UI, persisted in LocalStorage. Supports dynamic switching of `baseUrl` and database connections (`dbUrl`, `dbUsername`, `dbPassword`) without restarting the server. Sensitive variables are automatically encrypted on the server and masked in the UI.
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
- **Interactive JavaScript Debugger**: A step-through debugger for GraalJS script blocks in the Web UI, featuring breakpoints, Resume, Step Over, Step Into, and Step Out. It provides real-time execution point highlighting and gutter markers.
- **Test History & Dashboard**: Persists test execution results in a local H2 database. Features a visual dashboard showing success rates by file, execution time trends, common failure patterns, and a list of the slowest tests. Access it via the "📊 Dashboard" button in the header.
- **In-place Variables**: Support for file-scoped variables using the `@name = value` syntax.
- **Dynamic Variables**: Built-in support for dynamic values like `{{$uuid}}`, `{{$timestamp}}`, `{{$randomInt}}`, etc.
- **Markdown Reports**: Automatically generates detailed reports in Markdown format with pretty-printed JSON and left-aligned table headers.
- **Credential Masking**: Automatically masks `Authorization` headers with `*` in reports and logs to protect sensitive information.
- **Spring Security Integration**: Secured API endpoints with Basic Auth and the web UI with a custom Form Login. Includes logout functionality with automatic redirection to the login page.
- **Custom Login Page**: A user-friendly, inline login form that matches the application's look and feel, including automatic light and dark mode support.
- **Customizable Timeouts**: Set per-request timeouts using `@timeout` and `@connection-timeout` comments (e.g., `# @timeout 10 s`).
- **Execution Delay**: Use `sleep(ms)`, `setTimeout(callback, ms)`, and `clearTimeout(id)` in pre-request and post-request scripts to delay execution or handle timers.
- **Base64 Encoding/Decoding**: Standard `btoa()` and `atob()` methods available globally and via `Window` object for Base64 manipulation.
- **Global Variables & Headers**: Advanced management of global variables (`isEmpty()`, `clear()`, `clearAll()`) and support for global headers via `client.global.headers.set()` that apply to all subsequent requests.
- **XML XPath Support**: XML processing of `application/xml` or `text/xml` responses supports XPath expressions via `doc.xpath(expression)` in JavaScript blocks.
- **URLSearchParams Support**: Standard `URLSearchParams` object available in scripts for easy query string manipulation, supporting constructors (string, object, array), methods (`append`, `get`, `set`, etc.), and iterators.
- **Collection Iteration**: Automatically iterate over collection variables (e.g., `[1,2,3]`) in URLs, headers, or bodies, sending separate requests for each item. Access the current state using `request.iteration()` and `request.templateValue(index)`.
- **Crypto & JWT Support**: Integrated Crypto API providing hash functions (SHA-2, SHA-3), HMAC, and SubtleCrypto (RSA, ECDSA) for signing, verification, and encryption. Native JWT support via `jwt.sign`, `jwt.verify`, and `jwt.decode`. See [crypto.http](src/main/resources/httptestfiles/crypto.http) for examples.
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
