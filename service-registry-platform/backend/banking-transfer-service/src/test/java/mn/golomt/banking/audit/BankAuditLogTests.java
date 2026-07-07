package mn.golomt.banking.audit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
@Sql(
    statements = {
        "delete from ledger_entries",
        "delete from transfers",
        "delete from bank_audit_logs",
        "update accounts set balance = 1000000.00, status = 'ACTIVE' where account_no = '100000001'",
        "update accounts set balance = 500000.00, status = 'ACTIVE' where account_no = '100000002'"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class BankAuditLogTests {

    @Autowired
    private MockMvc mockMvc;

    private static RequestPostProcessor adminJwt() {
        return jwt()
            .jwt(token -> token.subject("admin").claim("displayName", "System Administrator").claim("role", "ADMIN"))
            .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor customerJwt(String username) {
        return jwt()
            .jwt(token -> token.subject(username).claim("displayName", username).claim("role", "VIEWER"))
            .authorities(new SimpleGrantedAuthority("ROLE_VIEWER"));
    }

    @Test
    void successfulTransferIsAuditedWithActorSnapshot() throws Exception {
        mockMvc.perform(post("/api/transfers")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000001",
                      "toAccountNo": "100000002",
                      "amount": 15000,
                      "description": "Audited transfer"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/audit-logs").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].action").value("TRANSFER_CREATED"))
            .andExpect(jsonPath("$.content[0].targetType").value("TRANSFER"))
            .andExpect(jsonPath("$.content[0].actorUsername").value("admin"))
            .andExpect(jsonPath("$.content[0].actorDisplayName").value("System Administrator"))
            .andExpect(jsonPath("$.content[0].actorRole").value("ADMIN"));
    }

    @Test
    void failedTransferIsAuditedDespiteTheError() throws Exception {
        mockMvc.perform(post("/api/transfers")
                .with(customerJwt("batbold"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000001",
                      "toAccountNo": "100000002",
                      "amount": 5000000,
                      "description": "Too much"
                    }
                    """))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/audit-logs").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].action").value("TRANSFER_FAILED"))
            .andExpect(jsonPath("$.content[0].actorUsername").value("batbold"));
    }

    @Test
    void accountManagementActionsAreAudited() throws Exception {
        mockMvc.perform(post("/api/accounts/{accountNo}/block", "100000001").with(adminJwt()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/accounts/{accountNo}/unblock", "100000001").with(adminJwt()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/audit-logs").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].action").value("ACCOUNT_UNBLOCKED"))
            .andExpect(jsonPath("$.content[1].action").value("ACCOUNT_BLOCKED"));
    }

    @Test
    void auditLogIsAdminOnly() throws Exception {
        mockMvc.perform(get("/api/audit-logs").with(customerJwt("batbold")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
