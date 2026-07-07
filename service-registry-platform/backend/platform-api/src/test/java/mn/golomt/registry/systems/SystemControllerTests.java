package mn.golomt.registry.systems;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
class SystemControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void systemsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/systems"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createSystemStoresAssignmentFields() throws Exception {
        String token = loginAndExtractToken();
        String key = uniqueKey("assignment-crud");

        MvcResult result = createSystem(token, key, "Assignment CRUD System " + key)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.systemKey").value(key))
            .andExpect(jsonPath("$.name").value("Assignment CRUD System " + key))
            .andExpect(jsonPath("$.type").value("INTERNAL"))
            .andExpect(jsonPath("$.valuationMnt").value(2500000))
            .andExpect(jsonPath("$.developerName").value("Registry Developer"))
            .andExpect(jsonPath("$.developerTeam").value("Platform Team"))
            .andExpect(jsonPath("$.inUse").value(true))
            .andExpect(jsonPath("$.environment").value("DEV"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andReturn();

        long id = readId(result);

        mockMvc.perform(get("/api/systems/{id}", id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.systemKey").value(key));
    }

    @Test
    void searchSupportsKeywordTypeDeveloperAndInUseFilters() throws Exception {
        String token = loginAndExtractToken();
        String key = uniqueKey("search-filter");
        createSystem(token, key, "Search Filter System " + key).andExpect(status().isCreated());

        mockMvc.perform(get("/api/systems")
                .header("Authorization", "Bearer " + token)
                .param("keyword", key)
                .param("type", "INTERNAL")
                .param("developer", "Registry")
                .param("inUse", "true")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.content[0].systemKey").value(key));
    }

    @Test
    void updateSystemChangesFieldsAndRelations() throws Exception {
        String token = loginAndExtractToken();
        String key = uniqueKey("update-system");
        long id = readId(createSystem(token, key, "Update System " + key)
            .andExpect(status().isCreated())
            .andReturn());

        mockMvc.perform(put("/api/systems/{id}", id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Updated System",
                      "type": "CORE",
                      "valuationMnt": 5000000,
                      "description": "Updated description",
                      "developerName": "Updated Developer",
                      "developerTeam": "Core Team",
                      "startDate": "2026-07-01",
                      "endDate": "2026-12-31",
                      "inUse": true,
                      "environment": "UAT",
                      "baseUrl": "http://localhost:8099",
                      "healthUrl": "http://localhost:8099/actuator/health",
                      "swaggerUrl": "http://localhost:8099/swagger-ui/index.html",
                      "repoUrl": "https://example.com/updated-system.git",
                      "status": "ACTIVE",
                      "relatedSystems": [
                        {
                          "targetSystemId": 1,
                          "relationType": "CALLS",
                          "description": "Calls banking transfer service"
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated System"))
            .andExpect(jsonPath("$.type").value("CORE"))
            .andExpect(jsonPath("$.environment").value("UAT"))
            .andExpect(jsonPath("$.relatedSystems[0].targetSystemId").value(1))
            .andExpect(jsonPath("$.relatedSystems[0].relationType").value("CALLS"));
    }

    @Test
    void deleteDisablesSystemInsteadOfHardDeleting() throws Exception {
        String token = loginAndExtractToken();
        String key = uniqueKey("delete-system");
        long id = readId(createSystem(token, key, "Delete System " + key)
            .andExpect(status().isCreated())
            .andReturn());

        mockMvc.perform(delete("/api/systems/{id}", id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/systems/{id}", id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inUse").value(false))
            .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void createRejectsInvalidDateRange() throws Exception {
        String token = loginAndExtractToken();
        String key = uniqueKey("invalid-date");

        mockMvc.perform(post("/api/systems")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "systemKey": "%s",
                      "name": "Invalid Date System",
                      "type": "DIGITAL",
                      "valuationMnt": 1000,
                      "startDate": "2026-12-31",
                      "endDate": "2026-01-01",
                      "inUse": true
                    }
                    """.formatted(key)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("startDate must be before or equal to endDate"));
    }

    @Test
    void updateRejectsDuplicateRelations() throws Exception {
        String token = loginAndExtractToken();
        String key = uniqueKey("duplicate-relation");
        long id = readId(createSystem(token, key, "Duplicate Relation System " + key)
            .andExpect(status().isCreated())
            .andReturn());

        mockMvc.perform(put("/api/systems/{id}", id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Duplicate Relation System",
                      "type": "CORE",
                      "valuationMnt": 5000000,
                      "description": "Duplicate relation check",
                      "developerName": "Updated Developer",
                      "developerTeam": "Core Team",
                      "startDate": "2026-07-01",
                      "endDate": "2026-12-31",
                      "inUse": true,
                      "environment": "UAT",
                      "status": "ACTIVE",
                      "relatedSystems": [
                        {
                          "targetSystemId": 1,
                          "relationType": "CALLS",
                          "description": "First relation"
                        },
                        {
                          "targetSystemId": 1,
                          "relationType": "CALLS",
                          "description": "Duplicate relation"
                        }
                      ]
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Duplicate related system entry: 1"));
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
                  "description": "System registry CRUD test",
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
