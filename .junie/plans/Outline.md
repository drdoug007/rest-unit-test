# Rest Unit Test

## Project Overview
This application reads an extended Jetbrains HTTPClient file to run unit tests against a Rest Server.
It uses the GraalJS engine to execute the JavaScript code before and after each test.
The Rest Server connects to an external JDBC database. Along with executing the JavaScript code. 
The .http files can include SQL code before and executing the Rest call.
This allows the JavaScript code to process the result set from the SQL Queries

This server will implement a REST API to allow for integration-based testing.
The API will read the .http files and execute the corresponding REST requests and SQL scripts based on the defined configuration.
It will then output the results in Markdown format. 

## Example .http file

``` http request 
< {%
    const dealer = "Joes Cars"
    request.variables.set("dealer", dealer)
%>
### Get Cars from Joe's Cars 
GET http://localhost:8080/api/car-dealer/cars?dealer={{dealer}}
> {%SQL
    # Comment - The default dataSource is the Spring JDBC Primary dataSource
    # The :dealer comes from the JavaScript request variable
    ### DataSource = default
    ### Query = "select * FROM car_dealer.car_dealer_cars WHERE dealer = :dealer
    ### ResultSet ||Id|Make|Model|Color||
%}

> {%
    client.test("Request executed successfully", function () {
        client.assert(response.status === 200, "Response status is not 200");
        # Result Set is a Json Array 
        const resultSet = client.global.get("ResultSet");
        client.assert(resultSet.length > 1, "No rows returned");
        
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
- 


