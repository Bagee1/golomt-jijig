package mn.golomt.banking.transfer;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
class TransferReversalTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void reversalRestoresBalancesAndLinksBothTransfers() throws Exception {
        long originalId = createTransfer(50000);

        MvcResult reversalResult = mockMvc.perform(post("/api/transfers/{id}/reversal", originalId)
                .with(adminJwt()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.fromAccountNo").value("100000002"))
            .andExpect(jsonPath("$.toAccountNo").value("100000001"))
            .andExpect(jsonPath("$.amount").value(50000.00))
            .andExpect(jsonPath("$.reversalOfTransferId").value(originalId))
            .andExpect(jsonPath("$.ledgerEntries.length()").value(2))
            .andReturn();
        long reversalId = objectMapper.readTree(reversalResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/accounts/{accountNo}", "100000001").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(1000000.00));

        mockMvc.perform(get("/api/accounts/{accountNo}", "100000002").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(500000.00));

        mockMvc.perform(get("/api/transfers/{id}", originalId).with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REVERSED"))
            .andExpect(jsonPath("$.reversedByTransferId").value(reversalId));
    }

    @Test
    void transferCannotBeReversedTwice() throws Exception {
        long originalId = createTransfer(50000);

        mockMvc.perform(post("/api/transfers/{id}/reversal", originalId).with(adminJwt()))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transfers/{id}/reversal", originalId).with(adminJwt()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TRANSFER_NOT_REVERSIBLE"));
    }

    @Test
    void reversalFailsWhenReceiverAlreadySpentTheMoney() throws Exception {
        long originalId = createTransfer(300000);

        // Receiver forwards almost everything: 800,000 - 700,000 = 100,000 left.
        mockMvc.perform(post("/api/transfers")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000002",
                      "toAccountNo": "100000001",
                      "amount": 700000,
                      "description": "Receiver spends the money"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transfers/{id}/reversal", originalId).with(adminJwt()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));

        mockMvc.perform(get("/api/transfers/{id}", originalId).with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void failedTransferCannotBeReversed() throws Exception {
        mockMvc.perform(post("/api/transfers")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000002",
                      "toAccountNo": "100000001",
                      "amount": 900000,
                      "description": "Fails on balance"
                    }
                    """))
            .andExpect(status().isBadRequest());

        MvcResult list = mockMvc.perform(get("/api/transfers").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].status").value("FAILED"))
            .andReturn();
        JsonNode json = objectMapper.readTree(list.getResponse().getContentAsString());
        long failedId = json.get("content").get(0).get("id").asLong();

        mockMvc.perform(post("/api/transfers/{id}/reversal", failedId).with(adminJwt()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TRANSFER_NOT_REVERSIBLE"));
    }

    @Test
    void nonAdminCannotReverse() throws Exception {
        long originalId = createTransfer(50000);

        mockMvc.perform(post("/api/transfers/{id}/reversal", originalId).with(customerJwt("batbold")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private long createTransfer(int amount) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/transfers")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000001",
                      "toAccountNo": "100000002",
                      "amount": %d,
                      "description": "Reversal test transfer"
                    }
                    """.formatted(amount)))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
