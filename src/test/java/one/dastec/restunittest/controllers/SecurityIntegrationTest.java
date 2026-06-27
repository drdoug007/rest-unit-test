package one.dastec.restunittest.controllers;

import one.dastec.restunittest.services.RestTestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import static org.mockito.Mockito.mock;

import one.dastec.restunittest.RestUnitTestApplication;

@SpringBootTest(classes = RestUnitTestApplication.class)
public class SecurityIntegrationTest {

    @Configuration
    static class TestConfig {
        @Bean
        @Primary
        public RestTestService restTestService() {
            return mock(RestTestService.class);
        }
    }

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RestTestService restTestService;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    public void unauthenticated_root_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void unauthenticated_api_shouldReturn401WithoutBasicAuthChallenge() throws Exception {
        mockMvc.perform(get("/api/tests"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    public void unauthenticated_static_shouldReturn200() throws Exception {
        mockMvc.perform(get("/scripts.js"))
                .andExpect(status().isOk());
    }

    @Test
    public void unauthenticated_wellKnown_shouldReturn404Not401Or302() throws Exception {
        // Should be permitted by security but return 404 because the file doesn't exist
        mockMvc.perform(get("/.well-known/anything"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    public void logout_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/login?logout")));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    public void authenticated_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/runtest/custom")
                        .contentType("application/json")
                        .content("{\"name\":\"test\",\"content\":\"GET http://localhost\"}"))
                .andExpect(status().isOk());
    }
}
