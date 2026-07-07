package mn.golomt.registry.securitycheck;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class SecurityCheckControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void controlsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/security-controls"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void controlsReturnSeededChecklist() throws Exception {
        String token = loginAndExtractToken();

        mockMvc.perform(get("/api/security-controls")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(8))
            .andExpect(jsonPath("$[0].controlKey").value("HTTPS_ENABLED"))
            .andExpect(jsonPath("$[0].weight").value(15));
    }

    @Test
    void systemChecksReturnNotCheckedDefaults() throws Exception {
        String token = loginAndExtractToken();

        mockMvc.perform(get("/api/systems/{systemId}/security-checks", 1)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(8))
            .andExpect(jsonPath("$[0].result").value("NOT_CHECKED"));
    }

    @Test
    void updateChecksStoresResultsAndCalculatesScore() throws Exception {
        String token = loginAndExtractToken();

        mockMvc.perform(put("/api/systems/{systemId}/security-checks", 1)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "checks": [
                        {
                          "controlId": 1,
                          "result": "PASS",
                          "evidence": "Uses HTTPS in production"
                        },
                        {
                          "controlId": 2,
                          "result": "WARNING",
                          "evidence": "Auth enabled, MFA not included yet"
                        },
                        {
                          "controlId": 3,
                          "result": "FAIL",
                          "evidence": "Role policy missing"
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(8))
            .andExpect(jsonPath("$[0].result").value("PASS"))
            .andExpect(jsonPath("$[0].evidence").value("Uses HTTPS in production"))
            .andExpect(jsonPath("$[1].result").value("WARNING"))
            .andExpect(jsonPath("$[2].result").value("FAIL"))
            .andExpect(jsonPath("$[0].checkedBy").value(1));

        mockMvc.perform(get("/api/systems/{systemId}/security-score", 1)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.systemId").value(1))
            .andExpect(jsonPath("$.totalWeight").value(100))
            .andExpect(jsonPath("$.earnedWeight").value(22.5))
            .andExpect(jsonPath("$.score").value(23))
            .andExpect(jsonPath("$.passCount").value(1))
            .andExpect(jsonPath("$.warningCount").value(1))
            .andExpect(jsonPath("$.failCount").value(1))
            .andExpect(jsonPath("$.notCheckedCount").value(5));
    }

    @Test
    void updateRejectsDuplicateControls() throws Exception {
        String token = loginAndExtractToken();

        mockMvc.perform(put("/api/systems/{systemId}/security-checks", 1)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "checks": [
                        {
                          "controlId": 1,
                          "result": "PASS"
                        },
                        {
                          "controlId": 1,
                          "result": "FAIL"
                        }
                      ]
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Duplicate security control entry: 1"));
    }

    @Test
    void batchScoresRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/security-scores"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void batchScoresReturnAllSystems() throws Exception {
        String token = loginAndExtractToken();

        mockMvc.perform(get("/api/security-scores")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(3)))
            .andExpect(jsonPath("$[0].systemId").value(1))
            .andExpect(jsonPath("$[0].totalWeight", greaterThanOrEqualTo(100)))
            .andExpect(jsonPath("$[0].notCheckedCount", greaterThanOrEqualTo(0)));
    }

    @Test
    void scoreForUnknownSystemReturnsNotFound() throws Exception {
        String token = loginAndExtractToken();

        mockMvc.perform(get("/api/systems/{systemId}/security-score", 99999)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("System not found: 99999"));
    }

    @Test
    void systemScoreReturnsAllNotCheckedBeforeUpdates() throws Exception {
        String token = loginAndExtractToken();

        mockMvc.perform(get("/api/systems/{systemId}/security-score", 2)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.score").value(0))
            .andExpect(jsonPath("$.totalWeight", greaterThanOrEqualTo(100)))
            .andExpect(jsonPath("$.notCheckedCount").value(8));
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
}

