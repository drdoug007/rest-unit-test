package one.dastec.restunittest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Environment environment = new Environment();

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public static class Environment {
        private String name;
        private String release;
        private String baseUrl;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRelease() {
            return release;
        }

        public void setRelease(String release) {
            this.release = release;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getBaseUrl() {
            return baseUrl;
        }
    }
}
