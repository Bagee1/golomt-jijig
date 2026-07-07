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
        "delete from accounts where account_no not in ('100000001', '100000002')",
        "delete from customers where customer_no not in ('CUST-0001', 'CUST-0002')",
        "update accounts set balance = 1000000.00, status = 'ACTIVE' where account_no = '100000001'",
        "update accounts set balance = 500000.00, status = 'ACTIVE' where account_no = '100000002'"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class TransferOwnershipTests {

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
    void customerCanTransferFromOwnAccount() throws Exception {
        mockMvc.perform(post("/api/transfers")
                .with(customerJwt("batbold"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000001",
                      "toAccountNo": "100000002",
                      "amount": 25000,
                      "description": "Own account transfer"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void customerCannotTransferFromForeignAccountAndNoRowIsPersisted() throws Exception {
        mockMvc.perform(post("/api/transfers")
                .with(customerJwt("batbold"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000002",
                      "toAccountNo": "100000001",
                      "amount": 25000,
                      "description": "Foreign account transfer"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN_ACCOUNT"));

        // Authorization failures are not business events: no FAILED row is written.
        mockMvc.perform(get("/api/transfers").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/accounts/{accountNo}", "100000002").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(500000.00));
    }

    @Test
    void customerSeesOnlyTransfersInvolvingOwnAccounts() throws Exception {
        // A transfer between the two seeded accounts involves both seeded customers.
        mockMvc.perform(post("/api/transfers")
                .with(customerJwt("batbold"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "100000001",
                      "toAccountNo": "100000002",
                      "amount": 10000,
                      "description": "Visible to both"
                    }
                    """))
            .andExpect(status().isCreated());

        // A transfer between two admin-created accounts of an unlinked customer
        // must stay invisible to seeded customers.
        String unlinkedFrom = openAccountForNewCustomer(300000);
        String unlinkedTo = openAccountForNewCustomer(0);
        MvcResult foreign = mockMvc.perform(post("/api/transfers")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "%s",
                      "toAccountNo": "%s",
                      "amount": 5000,
                      "description": "Unlinked customers only"
                    }
                    """.formatted(unlinkedFrom, unlinkedTo)))
            .andExpect(status().isCreated())
            .andReturn();
        long foreignId = objectMapper.readTree(foreign.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/transfers").with(customerJwt("batbold")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].description").value("Visible to both"));

        mockMvc.perform(get("/api/transfers").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/transfers/{id}", foreignId).with(customerJwt("batbold")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN_ACCOUNT"));
    }

    @Test
    void customerCannotViewForeignAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountNo}", "100000002").with(customerJwt("batbold")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN_ACCOUNT"));
    }

    @Test
    void myAccountsReturnsOnlyOwnAccounts() throws Exception {
        mockMvc.perform(get("/api/accounts/my").with(customerJwt("batbold")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].accountNo").value("100000001"));

        mockMvc.perform(get("/api/accounts/my").with(customerJwt("no-such-user")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    private String openAccountForNewCustomer(int initialBalance) throws Exception {
        MvcResult customer = mockMvc.perform(post("/api/customers")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "Test",
                      "lastName": "Customer"
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn();
        JsonNode customerJson = objectMapper.readTree(customer.getResponse().getContentAsString());

        MvcResult account = mockMvc.perform(post("/api/accounts")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerNo": "%s",
                      "initialBalance": %d
                    }
                    """.formatted(customerJson.get("customerNo").asText(), initialBalance)))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(account.getResponse().getContentAsString()).get("accountNo").asText();
    }
}
