package mn.golomt.registry.users;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCreatesUserAndNewUserCanLogin() throws Exception {
        String token = loginAndExtractToken("admin", "admin123");

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "Reg.Test-User1",
                      "password": "reg-pass-123",
                      "displayName": "Reg Test User",
                      "role": "VIEWER"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("reg.test-user1"))
            .andExpect(jsonPath("$.displayName").value("Reg Test User"))
            .andExpect(jsonPath("$.role").value("VIEWER"))
            .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "reg.test-user1",
                      "password": "reg-pass-123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.role").value("VIEWER"));
    }

    @Test
    void duplicateUsernameIsRejected() throws Exception {
        String token = loginAndExtractToken("admin", "admin123");

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "ADMIN",
                      "password": "whatever-123",
                      "displayName": "Duplicate Admin",
                      "role": "VIEWER"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Username already exists: admin"));
    }

    @Test
    void shortPasswordIsRejected() throws Exception {
        String token = loginAndExtractToken("admin", "admin123");

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "shortpass-user",
                      "password": "short",
                      "displayName": "Short Pass",
                      "role": "VIEWER"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void nonAdminCannotCreateUsers() throws Exception {
        mockMvc.perform(post("/api/users")
                .with(jwt()
                    .jwt(token -> token.subject("admin"))
                    .authorities(new SimpleGrantedAuthority("ROLE_VIEWER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "viewer-made-user",
                      "password": "whatever-123",
                      "displayName": "Should Fail",
                      "role": "VIEWER"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied"));
    }

    private String loginAndExtractToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s",
                      "password": "%s"
                    }
                    """.formatted(username, password)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }
}
