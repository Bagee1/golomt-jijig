package mn.golomt.registry.audit;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void auditLogsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void auditLogsReturnSystemAndSecurityActions() throws Exception {
        String token = loginAndExtractToken();
        String key = uniqueKey("audit-system");
        long id = readId(createSystem(token, key, "Audit System " + key)
            .andExpect(status().isCreated())
            .andReturn());

        updateSystem(token, id);
        updateSecurityChecks(token, id);

        mockMvc.perform(delete("/api/systems/{id}", id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/audit-logs")
                .header("Authorization", "Bearer " + token)
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(4)))
            .andExpect(jsonPath("$.content[*].action", hasItems(
                "SYSTEM_CREATED",
                "SYSTEM_UPDATED",
                "SECURITY_CHECK_UPDATED",
                "SYSTEM_DISABLED"
            )))
            .andExpect(jsonPath("$.content[0].actorUsername").value("admin"));
    }

    private org.springframework.test.web.servlet.ResultActions createSystem(
        String token,
        String key,
        String name
    ) throws Exception {
        return mockMvc.perform(post("/api/systems")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "systemKey": "%s",
                  "name": "%s",
                  "type": "INTERNAL",
                  "valuationMnt": 2500000,
                  "description": "Audit log test system",
                  "developerName": "Registry Developer",
                  "developerTeam": "Platform Team",
                  "startDate": "2026-07-06",
                  "endDate": "2026-12-31",
                  "inUse": true,
                  "environment": "DEV",
                  "baseUrl": "http://localhost:8098",
                  "healthUrl": "http://localhost:8098/actuator/health",
                  "swaggerUrl": "http://localhost:8098/swagger-ui/index.html",
                  "repoUrl": "https://example.com/system-registry.git",
                  "status": "ACTIVE",
                  "relatedSystems": []
                }
                """.formatted(key, name)));
    }

    private void updateSystem(String token, long id) throws Exception {
        mockMvc.perform(put("/api/systems/{id}", id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Updated Audit System",
                      "type": "CORE",
                      "valuationMnt": 5000000,
                      "description": "Updated for audit log test",
                      "developerName": "Updated Developer",
                      "developerTeam": "Core Team",
                      "startDate": "2026-07-01",
                      "endDate": "2026-12-31",
                      "inUse": true,
                      "environment": "UAT",
                      "status": "ACTIVE",
                      "relatedSystems": []
                    }
                    """))
            .andExpect(status().isOk());
    }

    private void updateSecurityChecks(String token, long id) throws Exception {
        mockMvc.perform(put("/api/systems/{systemId}/security-checks", id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "checks": [
                        {
                          "controlId": 1,
                          "result": "PASS",
                          "evidence": "Audit test evidence"
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk());
    }

    private String loginAndExtractToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "admin",
                      "password": "admin123"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    private String uniqueKey(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
