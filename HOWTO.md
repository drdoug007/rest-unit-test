# JavaScript in HTTP Client - HowTo

This document provides a comprehensive guide and examples for using JavaScript in your `.http` test files.

## Table of Contents
1. [Overview](#overview)
2. [The `client` Object](#the-client-object)
    - [Testing & Assertions](#testing--assertions)
    - [Logging](#logging)
    - [Global Variables](#global-variables)
    - [Global Headers](#global-headers)
3. [The `request` Object](#the-request-object)
    - [Request Variables](#request-variables)
    - [Collection Iteration](#collection-iteration)
4. [The `response` Object](#the-response-object)
    - [Response Properties](#response-properties)
    - [JSON Handling](#json-handling)
    - [XML/HTML Handling (DOM & XPath)](#xmlhtml-handling-dom--xpath)
5. [Crypto & JWT API](#crypto--jwt-api)
    - [Hash Functions](#hash-functions)
    - [HMAC](#hmac)
    - [SubtleCrypto (RSA & ECDSA)](#subtlecrypto-rsa--ecdsa)
    - [JWT (JSON Web Tokens)](#jwt-json-web-tokens)
6. [Utilities & Web APIs](#utilities--web-apis)
    - [Timers & Sleep](#timers--sleep)
    - [Base64 (atob/btoa)](#base64-atobbtoa)
    - [URLSearchParams](#urlsearchparams)
    - [Collection Iteration](#collection-iteration)
7. [The `markdowner` Helper](#the-markdowner-helper)
8. [SQL Database Access](#sql-database-access)
9. [Environment Management](#environment-management)
    - [Creating & Managing Environments](#creating--managing-environments)
    - [Variable Resolution & Priority](#variable-resolution--priority)
    - [Sensitive Variable Encryption](#sensitive-variable-encryption)
    - [Dynamic Database Switching](#dynamic-database-switching)
10. [Integrated Mock Server](#integrated-mock-server)
11. [Importing Collections (OpenAPI, Postman, Insomnia)](#importing-collections-openapi-postman-insomnia)
12. [End-to-End (E2E) Browser Testing](#end-to-end-e2e-browser-testing)

---

---

## Overview
JavaScript can be included in `.http` files using script blocks:
- `< {% ... %}`: Pre-request script (runs before the request).
- `> {% ... %}`: Response handler script (runs after the response is received).

Scripts are executed in a sandbox using GraalJS. Each block is wrapped in an IIFE to prevent variable name collisions between different request blocks.

---

## The `client` Object

The `client` object is used for testing, logging, and managing state across requests.

### Testing & Assertions
```javascript
> {%
    client.test("Status code is 200", () => {
        client.assert(response.status === 200, "Response status is not 200");
    });
    
    client.test("Content-Type is JSON", () => {
        client.assert(response.contentType.mimeType === "application/json");
    });
%}
```

### Logging
Logs appear in the "Logs" section of the generated Markdown report.
```javascript
< {%
    client.log("Preparing request for user ID: " + client.global.get("userId"));
%}
```

### Global Variables
Global variables persist across different `.http` files and execution flows.
```javascript
> {%
    client.global.set("auth_token", response.body.token);
    client.log("New token: " + client.global.get("auth_token"));
    
    if (client.global.isEmpty()) { /* ... */ }
    client.global.clear("old_variable");
    client.global.clearAll();
%}
```

### Global Headers
Global headers are automatically applied to the current (if set in pre-request) and all subsequent requests in the same execution flow.
```javascript
< {%
    client.global.headers.set("X-Session-ID", "session-123");
%}

// To remove a global header:
< {%
    client.global.headers.clear("X-Session-ID");
%}
```

---

## The `request` Object

### Request Variables
Variables defined in `request.variables` are scoped to the current request execution.
```javascript
< {%
    request.variables.set("temp_id", Math.random().toString(36).substring(7));
%}
GET {{baseUrl}}/api/items/{{temp_id}}
```

### Collection Iteration
If a variable is an array/list, the HTTP client will send a separate request for each item.
```javascript
< {%
    request.variables.set("ids", [101, 102, 103]);
%}
GET {{baseUrl}}/api/resource/{{ids}}

> {%
    client.log("Iteration: " + request.iteration()); // 0, 1, 2
    client.log("Value: " + request.templateValue(1)); // 101, 102, 103
%}
```

---

## The `response` Object

### Response Properties
- `response.status`: HTTP status code (Number).
- `response.headers`: Response headers (Object).
- `response.contentType`: Content-Type information (Object).
- `response.body`: Response body (Object/String/DOM).

### JSON Handling
If the response is JSON, `response.body` is automatically parsed into a JavaScript object.
```javascript
> {%
    let name = response.body.user.name;
    client.assert(name === "Alice");
%}
```

### XML/HTML Handling (DOM & XPath)
If the response is XML or HTML, `response.body` provides DOM methods.
```javascript
> {%
    // DOM Methods
    let title = response.body.getElementsByTagName("title")[0].textContent;
    let div = response.body.getElementById("main-content");
    
    // XPath Support
    let price = response.body.xpath("//item[@id='1']/price/text()")[0].textContent;
    client.log("Price is: " + price);
%}
```

---

## Crypto & JWT API

### Hash Functions
Supports SHA-2 and SHA-3 families.
```javascript
< {%
    const hash = crypto.sha256()
        .updateWithText("message")
        .digest().toHex();
%}
```

### HMAC
```javascript
< {%
    const signature = crypto.hmac.sha256()
        .withTextSecret("secret")
        .updateWithText("data")
        .digest().toHex();
%}
```

### SubtleCrypto (RSA & ECDSA)
Provides standard cryptographic functions (sign, verify, importKey, generateKey, exportKey).
```javascript
< {%
    const algorithm = { name: "RSA-PSS", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256" };
    const pair = crypto.subtle.generateKey(algorithm, true, ["sign", "verify"]);
    
    const signature = crypto.subtle.sign({ name: "RSA-PSS", saltLength: 32 }, pair.privateKey, data);
    const isValid = crypto.subtle.verify({ name: "RSA-PSS", saltLength: 32 }, pair.publicKey, signature, data);
%}
```

### JWT (JSON Web Tokens)
```javascript
< {%
    const token = jwt.sign({ sub: "user123" }, "secret", { algorithm: "HS256" });
    const verified = jwt.verify(token, "secret", { algorithm: "HS256" });
    const decoded = jwt.decode(token);
    client.log("Issuer: " + decoded.payload.iss.asString());
%}
```

---

## Utilities & Web APIs

### Timers & Sleep
Execution is delayed synchronously using `sleep` or asynchronously using `setTimeout`.
```javascript
< {%
    client.log("Waiting...");
    await sleep(2000); // Wait 2 seconds
    client.log("Resuming.");
    
    setTimeout(() => {
        client.log("Delayed log");
    }, 1000);
%}
```

### Base64 (atob/btoa)
```javascript
< {%
    const encoded = btoa("Hello");
    const decoded = atob(encoded);
%}
```

### URLSearchParams
Easy query string manipulation.
```javascript
< {%
    const params = new URLSearchParams("a=1&b=2");
    params.append("c", "3");
    client.global.set("query", params.toString());
%}
GET {{baseUrl}}/api?{{query}}
```

### Collection Iteration
You can send multiple requests by providing an array or list to a variable. The HTTP Client will iterate over the collection and send a separate request for each item.

Use `request.iteration()` to get the current loop index and `request.templateValue(index)` to get interpolated values.

Example:
```javascript
< {%
    request.variables.set("ids", [101, 102, 103]);
%}
GET {{baseUrl}}/api/items/{{ids}}

> {%
    client.log("Iteration: " + request.iteration());
    client.log("Value: " + request.templateValue(1));
%}
```
See `src/main/resources/httptestfiles/car_iteration.http` for a comprehensive example of adding multiple records with server-generated IDs.

---

## The `markdowner` Helper
Used to add custom content to the test report.
- `markdowner.heading(level, text)`
- `markdowner.table(headers, rows)`
- `markdowner.codeBlock(language, code, prettyPrint)`
- `markdowner.raw(text)`

```javascript
> {%
    markdowner.heading(3, "Custom Section");
    markdowner.table(["ID", "Name"], [["1", "Item A"], ["2", "Item B"]]);
    markdowner.codeBlock("json", JSON.stringify(response.body, null, 2));
    markdowner.raw("---"); // Horizontal rule
%}
```

---

## SQL Database Access

You can execute SQL queries directly against the configured database.

### `client.sqlQuery(sql)`
Executes a SELECT query and returns the result as an array of objects.
```javascript
< {%
    const sql = "SELECT id, name FROM categories WHERE active = true";
    const results = client.sqlQuery(sql);
    
    if (results.length > 0) {
        client.global.set("first_category_id", results[0].ID);
        client.log("Found " + results.length + " categories.");
    }
%}
```

### SQL Pretty Printing
When using `markdowner.codeBlock`, you can enable SQL pretty-printing for better readability in reports.
```javascript
> {%
    const query = "SELECT * FROM users WHERE email = 'test@example.com' AND status = 'active' ORDER BY created_at DESC";
    // The third parameter 'true' enables pretty-printing for SQL
    markdowner.codeBlock("sql", query, true);
%}
```

---

## Environment Management

The Environment Management feature allows you to define, manage, and switch between different sets of variables (e.g., Development, Staging, Production) without modifying your `.http` files.

### Creating & Managing Environments
1. **Quick Access**: Click the green **+** button next to the environment selector in the header to create a new environment.
2. **Management Modal**: Click the gear icon next to the environment selector to open the **Manage Environments** modal.
   - **Create**: Use the **+ New Environment** button.
   - **Rename**: Select an environment and edit its name; changes are reflected in real-time.
   - **Delete**: Click the trash icon next to an environment name.
   - **Variables**: Add key/value pairs in the table or use the **Switch to JSON** toggle to manage them in bulk using JSON format.
   - **Save**: Click **Save All** to persist changes to `LocalStorage`.

### Variable Resolution & Priority
When a request is executed, variables are resolved in the following order of priority (highest to lowest):
1. **Environment Variables**: Defined in the currently selected environment.
2. **Global Variables**: Managed via the **Global Variables** modal (persistent across files).
3. **Request Variables**: Defined within the `.http` file using `@name = value` or `request.variables.set()`.
4. **Application Globals**: Defined in the server's `application.yaml` under `app.test-globals`.

### Sensitive Variable Encryption
Sensitive variables (e.g., `password`, `apiKey`, `dbPassword`, `token`) are automatically identified and protected:
- **UI Masking**: Values appear as `********` in the management table.
- **Server-Side Encryption**: Values are sent to the server for AES encryption before being stored in the browser's `LocalStorage`.
- **Automatic Decryption**: The test runner automatically decrypts these values (prefixed with `{enc}`) before using them in requests or scripts.

### Dynamic Database Switching
You can target different databases for your tests by providing specific variables in your environment:
- `dbUrl`: The JDBC URL of the target database (supports PostgreSQL, MySQL, and H2).
- `dbUsername`: The database username.
- `dbPassword`: The database password (automatically encrypted).

If these variables are present, `client.sqlQuery()` and legacy SQL blocks will automatically connect to the specified database instead of the application's default data source.

---

## Integrated Mock Server

The Integrated Mock Server allows you to define simulated API responses directly within your `.http` files. This is useful when external APIs are unstable, unavailable, or when you want to test specific error scenarios.

### Defining a Mock Response
To define a mock response, simply add the desired HTTP response syntax after your request:

```http
### Get User with Mock
GET http://external-api.com/users/1

HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": 1,
  "name": "Mocked User",
  "status": "{{userStatus}}"
}

> {%
client.test("Check mocked response", function() {
    client.assert(response.body.name === "Mocked User");
});
%}
```

### Key Features
- **Interception**: The test runner detects the mock definition and intercepts the request, returning your defined response instead of making a real network call.
- **Variable Resolution**: Variables in the mock status, headers, and body are automatically resolved using the current test context (environment, global, and local variables).
- **Format**: Supports any standard HTTP response headers and body content.
- **Visual Feedback**: The test report will indicate when a "Mock Response Intercepted" was used.

---

## Visual Assertion Builder

The Visual Assertion Builder allows you to quickly generate assertions by clicking on elements in the response view.

### Usage
1. Run a test to see the response in the report panel.
2. In the response body (JSON or XML), click on a key (JSON) or a tag name (XML).
3. An assertion snippet will be automatically appended to the post-script block (`> {% ... %}`) in the source editor.

### Example
If you click on `"id"` in a JSON response:
```json
{
  "id": 123
}
```
The following code will be added to your `.http` file:
```javascript
> {%
    client.test("Check id", () => {
        client.assert(response.body.id === 123, "Expected id to be 123");
    });
%}
```

### Key Features
- **Smart JSON Paths**: Automatically detects nested paths (e.g., `response.body.user.profile.name`).
- **XML Support**: Generates DOM-based assertions using `getElementsByTagName`.
- **Instant Feedback**: A toast notification confirms when an assertion has been added.
- **Automatic Script Blocks**: Creates a new post-script block if one doesn't exist, or appends to the current one.

---

## Importing Collections (OpenAPI, Postman, Insomnia)

You can import existing API collections from various formats to quickly generate `.http` test files.

### Supported Formats
- **OpenAPI 3.0 (JSON/YAML)**: Imports paths, methods, headers, and generates request body examples from schemas.
- **Postman Collections (v2.1)**: Imports requests, folders, headers, and basic pre-request/test scripts.
- **Insomnia Exports (v4)**: Imports requests and environment variables.

### How to Import
1. Click the green **Import** button in the sidebar.
2. Select your preferred method:
   - **From File**: Upload a `.json`, `.yaml`, or `.yml` file.
   - **From URL**: Provide a direct link to the specification.
   - **Paste**: Paste the raw JSON/YAML content into the prompt.
3. The tool will automatically detect the format and convert it into a new custom `.http` test.
4. Review the generated test, make any necessary adjustments, and click **Save** or **Run**.

### Postman Script Mapping
The importer makes a "best-effort" attempt to map Postman scripts to **rest-unit-test** syntax:
- `pm.test(...)` → `client.test(...)`
- `pm.expect(...)` → `client.assert(...)`
- `pm.response.json()` → `response.body`
- `pm.environment.set(...)` → `client.global.set(...)`
- Status code checks are also automatically converted.

---

## End-to-End (E2E) Browser Testing

The project uses **Playwright** for headless browser testing to ensure the Web UI remains functional and bug-free.

### Prerequisites
- **JDK 25** (already required for the main application).
- **Playwright Maven Dependency** (included in `pom.xml`).

### Running E2E Tests
To execute all E2E tests, use the following Maven command:
```bash
./mvnw test -Dtest=WebUI*Test
```

### Writing New E2E Tests
New E2E tests should extend the `BaseE2ETest` class, which handles the Playwright lifecycle (launching the browser, creating contexts, etc.) and provides a helper `login()` method.

**Example Test:**
```java
public class WebUILoginTest extends BaseE2ETest {
    @Test
    void testLoginAndDashboard() {
        login("user", "password");
        assertThat(page).hasTitle("REST Unit Test Runner");
        assertThat(page.locator(".sidebar")).isVisible();
    }
}
```

### Configuration
E2E tests use `@SpringBootTest` with a random port. The `BaseE2ETest` automatically configures a default test user with credentials `user`/`password` for security-enabled test runs.

#### Headless vs. Headed Mode
By default, tests run in **headless** mode (no visible browser window) for speed and CI/CD compatibility.

To run tests in **headed** mode (visible browser window) for local development or debugging:
```bash
./mvnw test -Dtest=WebUI*Test -Dheadless=false
```

#### Known Limitations
- **CM6 Editor Interaction**: Interacting with the CodeMirror 6 editor (e.g., in "New Test" or "Edit" modes) is brittle in headless mode. Tests that require editor interaction are automatically skipped when `headless=true` is detected. Run these tests in **headed mode** (`-Dheadless=false`) for reliable results.