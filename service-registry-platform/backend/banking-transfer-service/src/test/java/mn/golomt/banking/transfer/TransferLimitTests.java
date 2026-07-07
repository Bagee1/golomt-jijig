package mn.golomt.banking.transfer;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.limits.max-per-transfer=50000.00",
    "app.limits.daily-outgoing-total=80000.00"
})
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
class TransferLimitTests {

    @Autowired
    private MockMvc mockMvc;

    private static RequestPostProcessor adminJwt() {
        return jwt()
            .jwt(token -> token.subject("admin").claim("displayName", "System Administrator").claim("role", "ADMIN"))
            .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test
    void transferAbovePerTransferLimitIsRejectedAndPersistedAsFailed() throws Exception {
        mockMvc.perform(post("/api/transfers")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000001",
                      "toAccountNo": "100000002",
                      "amount": 60000,
                      "description": "Above per-transfer limit"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("LIMIT_EXCEEDED"));

        mockMvc.perform(get("/api/accounts/{accountNo}", "100000001").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(1000000.00));

        mockMvc.perform(get("/api/transfers").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].status").value("FAILED"))
            .andExpect(jsonPath("$.content[0].failureReason").value("LIMIT_EXCEEDED"));
    }

    @Test
    void dailyOutgoingLimitAccumulatesAcrossTransfers() throws Exception {
        createTransfer(50000);
        createTransfer(25000);

        // 50,000 + 25,000 already sent today; another 25,000 would exceed the 80,000 cap.
        mockMvc.perform(post("/api/transfers")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000001",
                      "toAccountNo": "100000002",
                      "amount": 25000,
                      "description": "Breaks daily limit"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("LIMIT_EXCEEDED"));

        mockMvc.perform(get("/api/accounts/{accountNo}", "100000001").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(925000.00));

        mockMvc.perform(get("/api/transfers").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void failedTransfersDoNotCountTowardsDailyLimit() throws Exception {
        createTransfer(50000);

        // A rejected attempt (per-transfer limit) must not consume daily allowance.
        mockMvc.perform(post("/api/transfers")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000001",
                      "toAccountNo": "100000002",
                      "amount": 60000,
                      "description": "Fails on per-transfer limit"
                    }
                    """))
            .andExpect(status().isBadRequest());

        // 30,000 still fits: 50,000 succeeded + 30,000 = 80,000 (limit inclusive).
        createTransfer(30000);
    }

    private void createTransfer(int amount) throws Exception {
        mockMvc.perform(post("/api/transfers")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000001",
                      "toAccountNo": "100000002",
                      "amount": %d,
                      "description": "Limit test transfer"
                    }
                    """.formatted(amount)))
            .andExpect(status().isCreated());
    }
}
