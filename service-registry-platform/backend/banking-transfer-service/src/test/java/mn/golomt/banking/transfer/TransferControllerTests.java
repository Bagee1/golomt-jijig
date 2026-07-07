package mn.golomt.banking.transfer;

import static org.assertj.core.api.Assertions.assertThat;
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
class TransferControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static RequestPostProcessor platformJwt() {
        return jwt()
            .jwt(token -> token.subject("admin").claim("displayName", "System Administrator").claim("role", "ADMIN"))
            .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test
    void accountEndpointRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountNo}", "100000001"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void transferEndpointRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000001",
                      "toAccountNo": "100000002",
                      "amount": 1000,
                      "description": "No token"
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void accountEndpointReturnsSeededAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountNo}", "100000001").with(platformJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountNo").value("100000001"))
            .andExpect(jsonPath("$.customerNo").value("CUST-0001"))
            .andExpect(jsonPath("$.customerName").value("Bat Bold"))
            .andExpect(jsonPath("$.currency").value("MNT"))
            .andExpect(jsonPath("$.balance").value(1000000.00))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void transferMovesMoneyAndCreatesLedgerEntries() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/transfers")
                .with(platformJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000001",
                      "toAccountNo": "100000002",
                      "amount": 50000,
                      "description": "Demo transfer"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferRef").isString())
            .andExpect(jsonPath("$.fromAccountNo").value("100000001"))
            .andExpect(jsonPath("$.toAccountNo").value("100000002"))
            .andExpect(jsonPath("$.amount").value(50000.00))
            .andExpect(jsonPath("$.currency").value("MNT"))
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.ledgerEntries.length()").value(2))
            .andExpect(jsonPath("$.ledgerEntries[0].entryType").value("DEBIT"))
            .andExpect(jsonPath("$.ledgerEntries[0].balanceAfter").value(950000.00))
            .andExpect(jsonPath("$.ledgerEntries[1].entryType").value("CREDIT"))
            .andExpect(jsonPath("$.ledgerEntries[1].balanceAfter").value(550000.00))
            .andReturn();

        long transferId = readId(result);

        mockMvc.perform(get("/api/accounts/{accountNo}", "100000001").with(platformJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(950000.00));

        mockMvc.perform(get("/api/accounts/{accountNo}", "100000002").with(platformJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(550000.00));

        mockMvc.perform(get("/api/transfers/{id}", transferId).with(platformJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(transferId))
            .andExpect(jsonPath("$.ledgerEntries.length()").value(2));
    }

    @Test
    void transferRejectsInsufficientBalanceAndKeepsBalancesUntouched() throws Exception {
        mockMvc.perform(post("/api/transfers")
                .with(platformJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000002",
                      "toAccountNo": "100000001",
                      "amount": 900000,
                      "description": "Should fail"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"))
            .andExpect(jsonPath("$.message").value("Insufficient balance for account: 100000002"));

        mockMvc.perform(get("/api/accounts/{accountNo}", "100000001").with(platformJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(1000000.00));

        mockMvc.perform(get("/api/accounts/{accountNo}", "100000002").with(platformJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(500000.00));

        // The rejected attempt is persisted as an auditable FAILED transfer with no
        // ledger entries and no balance mutation.
        mockMvc.perform(get("/api/transfers").with(platformJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].status").value("FAILED"))
            .andExpect(jsonPath("$.content[0].failureReason").value("INSUFFICIENT_FUNDS"))
            .andExpect(jsonPath("$.content[0].ledgerEntries.length()").value(0));
    }

    @Test
    void transferRejectsSameAccount() throws Exception {
        mockMvc.perform(post("/api/transfers")
                .with(platformJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000001",
                      "toAccountNo": "100000001",
                      "amount": 1000,
                      "description": "Same account"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("SAME_ACCOUNT"))
            .andExpect(jsonPath("$.message").value("Cannot transfer to the same account"));
    }

    @Test
    void transferWithSameIdempotencyKeyExecutesOnlyOnce() throws Exception {
        String body = """
            {
              "fromAccountNo": "100000001",
              "toAccountNo": "100000002",
              "amount": 30000,
              "description": "Idempotent transfer"
            }
            """;

        MvcResult first = mockMvc.perform(post("/api/transfers")
                .with(platformJwt())
                .header("Idempotency-Key", "demo-key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn();

        MvcResult replay = mockMvc.perform(post("/api/transfers")
                .with(platformJwt())
                .header("Idempotency-Key", "demo-key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode firstJson = objectMapper.readTree(first.getResponse().getContentAsString());
        JsonNode replayJson = objectMapper.readTree(replay.getResponse().getContentAsString());
        assertThat(replayJson.get("transferRef").asText()).isEqualTo(firstJson.get("transferRef").asText());

        mockMvc.perform(get("/api/accounts/{accountNo}", "100000001").with(platformJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(970000.00));

        mockMvc.perform(get("/api/transfers").with(platformJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void transfersWithDifferentIdempotencyKeysExecuteSeparately() throws Exception {
        for (String key : new String[] {"key-a", "key-b"}) {
            mockMvc.perform(post("/api/transfers")
                    .with(platformJwt())
                    .header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "fromAccountNo": "100000001",
                          "toAccountNo": "100000002",
                          "amount": 10000,
                          "description": "Separate transfer"
                        }
                        """))
                .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/accounts/{accountNo}", "100000001").with(platformJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(980000.00));

        mockMvc.perform(get("/api/transfers").with(platformJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void transferRejectsTooLongIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/transfers")
                .with(platformJwt())
                .header("Idempotency-Key", "x".repeat(81))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000001",
                      "toAccountNo": "100000002",
                      "amount": 1000,
                      "description": "Key too long"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void transferListReturnsNewestTransfers() throws Exception {
        createTransfer("100000001", "100000002", 10000);

        mockMvc.perform(get("/api/transfers")
                .with(platformJwt())
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].fromAccountNo").value("100000001"))
            .andExpect(jsonPath("$.content[0].toAccountNo").value("100000002"))
            .andExpect(jsonPath("$.content[0].ledgerEntries.length()").value(2));
    }

    private void createTransfer(String fromAccountNo, String toAccountNo, int amount) throws Exception {
        mockMvc.perform(post("/api/transfers")
                .with(platformJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "%s",
                      "toAccountNo": "%s",
                      "amount": %d,
                      "description": "List test transfer"
                    }
                    """.formatted(fromAccountNo, toAccountNo, amount)))
            .andExpect(status().isCreated());
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }
}
