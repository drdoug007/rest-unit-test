# Rest Unit Test

## Project Overview
This application reads an extended Jetbrains HTTPClient file to run unit tests against a Rest Server.
It uses the GraalJS engine to execute the JavaScript code before and after each test.
The Rest Server connects to an external JDBC database. In addition to executing JavaScript code, `.http` files can include SQL queries both before and after the REST call. This allows scripts to verify database state or use query results to drive test logic.

This server implements a REST API to allow for integration-based testing. It reads `.http` files and executes the corresponding REST requests and SQL scripts based on the defined configuration, outputting the results in a detailed Markdown format. 

## Example .http file

```http request
### Get Cars from Joe's Cars 
< {%
    const dealer = "Joes Cars"
    client.global.set("dealer", dealer)
%}
GET {{baseUrl}}/api/cardealer/cars?dealer={{dealer}}
Authorization: Basic {{username}} {{password}}

> {%
    client.test("Request executed successfully", function () {
        client.assert(response.status === 200, "Response status is not 200");
    });

    const sql = "SELECT id, make, model, color FROM car_dealer_cars WHERE dealer = '" + client.global.get("dealer") + "'";
    const result = client.sqlQuery(sql);
    
    markdowner.heading(3, "Database Verification");
    markdowner.codeBlock("sql", sql, true);
    markdowner.table(result.columns, result.data);

    client.test("SQL query returned results", function () {
        // Result set data is a Json Array
        client.assert(result.data.length > 0, "No rows returned from database");
    });
%}
```

## Dependencies
- Java 25 - GraalVM 
- GraalJS (JavaScript engine) version 25+
- JDBC (Java Database Connectivity) 
- Spring Boot (Framework for the application) latest v4.1.0
- [x] Implement implementation
- [x] Create unit tests for this project.
- [x] Include date and time in test reports.


