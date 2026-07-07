package mn.golomt.registry.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginLockoutTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @AfterEach
    void clearAdminAttempts() {
        loginAttemptService.recordSuccess("admin");
    }

    @Test
    void locksAccountAfterFiveFailedAttempts() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            performLogin("admin", "wrong-password").andExpect(status().isUnauthorized());
        }

        performLogin("admin", "admin123")
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.message").value("Too many failed login attempts. Try again later."));
    }

    @Test
    void successfulLoginResetsFailureCount() throws Exception {
        for (int attempt = 0; attempt < 4; attempt++) {
            performLogin("admin", "wrong-password").andExpect(status().isUnauthorized());
        }

        performLogin("admin", "admin123").andExpect(status().isOk());

        performLogin("admin", "wrong-password").andExpect(status().isUnauthorized());
        performLogin("admin", "admin123").andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions performLogin(String username, String password)
        throws Exception {
        return mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password)));
    }
}
