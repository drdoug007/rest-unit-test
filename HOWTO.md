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
    client.log("Value: " + request.templateValue(0)); // 101, 102, 103
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
    client.log("Value: " + request.templateValue(0));
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
