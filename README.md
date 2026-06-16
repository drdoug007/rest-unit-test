# Rest Unit Test Framework

A Spring Boot-based framework for executing HTTP-based unit tests with integrated SQL verification and Markdown report generation.

## Features

- **HTTP Request Execution**: Supports standard HTTP methods (GET, POST, etc.) with variable resolution.
- **SQL Verification**: Execute SQL queries against a database to verify state changes or fetch data for assertions.
- **JavaScript Assertions**: Use GraalJS to write powerful test logic and assertions in JavaScript.
- **Markdown Reports**: Automatically generates detailed test reports in Markdown format.
- **Custom Markdown Helper**: A `markdowner` helper available in JavaScript to add headings, tables, and code blocks directly to reports.

## Getting Started

### Prerequisites

- JDK 17 or higher
- PostgreSQL (or your preferred database)

### Configuration

Update `src/main/resources/application.yaml` with your database connection details.

### Running Tests

Tests are defined in `.http` files within `src/main/resources/httptestfiles/`. 

To run a test, use the following endpoint:
`GET /api/runtest/{testName}`

Example: `http://localhost:8099/api/runtest/cardealer`

## HTTP Test File Format

```http
### Test Description
GET http://example.com/api/resource
Content-Type: application/json

{
  "key": "value"
}

# < SQL
### Query = "SELECT * FROM my_table"
# SQL

> {%
    client.test("Status is 200", function() {
        client.assert(response.status === 200, "Response status is not 200");
    });

    const data = client.global.get("ResultSet");
    markdowner.heading(3, "Database Results");
    markdowner.table(["id", "name"], data);
%}
```

## Contributing

Please see [AGENTS.md](AGENTS.md) for details on our automated development process.
