package mn.golomt.registry.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// securityCanUpdateSecurityChecks writes results for system 1; clean them up so later
// test classes sharing the same H2 database still see NOT_CHECKED defaults.
@Sql(
    statements = "delete from security_check_results",
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class RoleAccessTests {

    private static final String VALID_SYSTEM_JSON = """
        {
          "name": "Role Test System",
          "type": "INTERNAL",
          "valuationMnt": 1000
        }
        """;

    private static final String VALID_CHECKS_JSON = """
        {
          "checks": [
            {
              "controlId": 1,
              "result": "PASS"
            }
          ]
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    private static RequestPostProcessor withRole(String role) {
        return jwt()
            .jwt(token -> token.subject("admin"))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void viewerCanListSystems() throws Exception {
        mockMvc.perform(get("/api/systems").with(withRole("VIEWER")))
            .andExpect(status().isOk());
    }

    @Test
    void viewerCannotCreateSystem() throws Exception {
        mockMvc.perform(post("/api/systems")
                .with(withRole("VIEWER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_SYSTEM_JSON))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void securityCannotCreateSystem() throws Exception {
        mockMvc.perform(post("/api/systems")
                .with(withRole("SECURITY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_SYSTEM_JSON))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void viewerCannotUpdateSecurityChecks() throws Exception {
        mockMvc.perform(put("/api/systems/{systemId}/security-checks", 1)
                .with(withRole("VIEWER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_CHECKS_JSON))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void securityCanUpdateSecurityChecks() throws Exception {
        mockMvc.perform(put("/api/systems/{systemId}/security-checks", 1)
                .with(withRole("SECURITY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_CHECKS_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void viewerCannotReadAuditLogs() throws Exception {
        mockMvc.perform(get("/api/audit-logs").with(withRole("VIEWER")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void securityCanReadAuditLogs() throws Exception {
        mockMvc.perform(get("/api/audit-logs").with(withRole("SECURITY")))
            .andExpect(status().isOk());
    }
}
