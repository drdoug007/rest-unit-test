# Implementing CI/CD with Jenkins

To implement a CI/CD pipeline for **rest-unit-test** using Jenkins, you should leverage the existing Maven/JUnit integration. This approach ensures that your HTTP tests are treated as first-class citizens in your build process, failing the build if assertions fail and providing detailed Markdown reports as artifacts.

### Prerequisites
1.  **Jenkins Environment**:
    - **JDK 25**: The project requires Java 25 (GraalVM is recommended for optimal JavaScript performance).
    - **Maven**: Ensure Maven is installed and configured in your Jenkins global tool configuration.
2.  **JVM Arguments**: The framework requires specific flags for native access and GraalJS execution. These are already configured in the project's `pom.xml`, but ensure the environment allows them:
    ```bash
    --enable-native-access=ALL-UNNAMED -XX:+EnableDynamicAgentLoading -Xshare:off --sun-misc-unsafe-memory-access=allow
    ```

---

### Implementation Steps

#### 1. The JUnit Wrapper (`CIHttpTests.java`)
The project contains a `CIHttpTests.java` class designed for CI environments. It starts the application on a random port, resolves the `baseUrl` dynamically, and executes the specified `.http` test suite.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CIHttpTests {
    @Autowired
    private RestTestService restTestService;

    @LocalServerPort
    private int port;

    @Test
    public void runCiTests() {
        Map<String, Object> globals = new HashMap<>();
        globals.put("baseUrl", "http://localhost:" + port);
        
        // Executes the ci-test.http file
        String report = restTestService.runTestWithContent("ci-test", restTestService.getTestSource("ci-test"), globals);
        
        // Log the report to console for Jenkins output
        System.out.println("CI Test Report:\n" + report);
        
        // Assertions: Fail the test if the report contains the error marker ❌
        assertFalse(report.contains("❌"), "HTTP Tests failed! See report above.");
    }
}
```

#### 2. Jenkins Pipeline Configuration (`Jenkinsfile`)
A `Jenkinsfile` is provided in the repository root to define the pipeline stages.

```groovy
pipeline {
    agent any

    tools {
        jdk 'jdk-25' // Match the name in Jenkins Global Tool Configuration
        maven 'maven-3.9'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                // Run only the CIHttpTests to verify API health
                sh './mvnw test -Dtest=CIHttpTests'
            }
        }
    }

    post {
        always {
            // Archive JUnit results to see them in Jenkins UI
            junit '**/target/surefire-reports/*.xml'
            
            // Archive the console output or specific report files as artifacts
            archiveArtifacts artifacts: '**/target/surefire-reports/*.txt', allowEmptyArchive: true
        }
    }
}
```

---

### Key Jenkins Features for Rest Unit Test

#### 📊 Visualizing Results
Since the test results are logged to the console in Markdown format, you can find the detailed report (including request/response headers and bodies) directly in the **Jenkins Console Output**. 

#### 🛠️ Handling Database Dependencies
If your tests use `client.sqlQuery()`, ensure the Jenkins agent has access to a test database. You can use the **Jenkins Docker Pipeline** plugin to spin up a database container (e.g., PostgreSQL) during the build.

#### 🔐 Credentials Management
Use Jenkins Credentials to securely pass sensitive information (like `username` and `password` for `Authorization` headers) into the test via environment variables or a `spring-boot` configuration:

```bash
./mvnw test -Dtest=CIHttpTests -Dapp.test-globals.username=${USER} -Dapp.test-globals.password=${PASS}
```

### Summary Checklist
- [x] Ensure **JDK 25** is available on Jenkins agents.
- [x] Use `./mvnw test -Dtest=CIHttpTests` for focused execution.
- [x] Verify `pom.xml` contains the required `argLine` in `maven-surefire-plugin`.
- [x] Archive artifacts to preserve the Markdown reports for audit and debugging.
