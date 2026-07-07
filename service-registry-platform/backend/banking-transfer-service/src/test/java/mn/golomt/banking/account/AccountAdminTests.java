package mn.golomt.banking.account;

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
        "delete from accounts where account_no not in ('100000001', '100000002')",
        "delete from customers where customer_no not in ('CUST-0001', 'CUST-0002')",
        "update accounts set balance = 1000000.00, status = 'ACTIVE' where account_no = '100000001'",
        "update accounts set balance = 500000.00, status = 'ACTIVE' where account_no = '100000002'"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class AccountAdminTests {

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
    void adminOpensAccountForCustomer() throws Exception {
        mockMvc.perform(post("/api/accounts")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerNo": "CUST-0001",
                      "initialBalance": 75000
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerNo").value("CUST-0001"))
            .andExpect(jsonPath("$.currency").value("MNT"))
            .andExpect(jsonPath("$.balance").value(75000.00))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        // The new account belongs to batbold and shows up in /my.
        mockMvc.perform(get("/api/accounts/my").with(customerJwt("batbold")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void openingAccountForUnknownCustomerFails() throws Exception {
        mockMvc.perform(post("/api/accounts")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerNo": "CUST-9999"
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
    }

    @Test
    void blockedAccountRejectsTransfersUntilUnblocked() throws Exception {
        mockMvc.perform(post("/api/accounts/{accountNo}/block", "100000001").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("BLOCKED"));

        mockMvc.perform(post("/api/transfers")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000001",
                      "toAccountNo": "100000002",
                      "amount": 1000,
                      "description": "From blocked account"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ACCOUNT_INACTIVE"));

        mockMvc.perform(post("/api/accounts/{accountNo}/unblock", "100000001").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/transfers")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000001",
                      "toAccountNo": "100000002",
                      "amount": 1000,
                      "description": "After unblock"
                    }
                    """))
            .andExpect(status().isCreated());
    }

    @Test
    void invalidStatusTransitionsAreRejected() throws Exception {
        mockMvc.perform(post("/api/accounts/{accountNo}/unblock", "100000001").with(adminJwt()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void closingRequiresZeroBalanceAndIsTerminal() throws Exception {
        mockMvc.perform(post("/api/accounts/{accountNo}/close", "100000001").with(adminJwt()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_EMPTY"));

        String emptyAccountNo = openAccount("CUST-0002", 0);
        mockMvc.perform(post("/api/accounts/{accountNo}/close", emptyAccountNo).with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(post("/api/accounts/{accountNo}/close", emptyAccountNo).with(adminJwt()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        mockMvc.perform(post("/api/accounts/{accountNo}/block", emptyAccountNo).with(adminJwt()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void adminListFiltersByCustomer() throws Exception {
        mockMvc.perform(get("/api/accounts").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/accounts").with(adminJwt()).param("customerNo", "CUST-0001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].accountNo").value("100000001"));
    }

    @Test
    void nonAdminCannotUseAccountManagementEndpoints() throws Exception {
        mockMvc.perform(get("/api/accounts").with(customerJwt("batbold")))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/accounts")
                .with(customerJwt("batbold"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerNo\": \"CUST-0001\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/accounts/{accountNo}/block", "100000001").with(customerJwt("batbold")))
            .andExpect(status().isForbidden());
    }

    private String openAccount(String customerNo, int initialBalance) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerNo": "%s",
                      "initialBalance": %d
                    }
                    """.formatted(customerNo, initialBalance)))
            .andExpect(status().isCreated())
            .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accountNo").asText();
    }
}
