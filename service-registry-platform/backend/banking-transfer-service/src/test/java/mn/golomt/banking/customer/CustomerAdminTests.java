package mn.golomt.banking.customer;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class CustomerAdminTests {

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
    void adminCreatesUpdatesAndDeactivatesCustomer() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/customers")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "Dulmaa",
                      "lastName": "Tseren",
                      "phone": "99112233",
                      "email": "dulmaa@example.mn",
                      "username": "dulmaa"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerNo").isString())
            .andExpect(jsonPath("$.username").value("dulmaa"))
            .andExpect(jsonPath("$.active").value(true))
            .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/customers/{id}", id)
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "Dulmaa",
                      "lastName": "Tserendorj",
                      "phone": "99112233",
                      "email": "dulmaa@example.mn",
                      "username": "dulmaa"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lastName").value("Tserendorj"));

        mockMvc.perform(post("/api/customers/{id}/deactivate", id).with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/customers/{id}", id).with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.accounts.length()").value(0));
    }

    @Test
    void duplicateUsernameIsRejected() throws Exception {
        mockMvc.perform(post("/api/customers")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "Fake",
                      "lastName": "Batbold",
                      "username": "batbold"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("USERNAME_TAKEN"));
    }

    @Test
    void customerSearchFindsByNameAndNumber() throws Exception {
        mockMvc.perform(get("/api/customers").with(adminJwt()).param("q", "bat"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].customerNo").value("CUST-0001"));

        mockMvc.perform(get("/api/customers").with(adminJwt()).param("q", "CUST-0002"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void customerDetailIncludesAccounts() throws Exception {
        MvcResult list = mockMvc.perform(get("/api/customers").with(adminJwt()).param("q", "CUST-0001"))
            .andExpect(status().isOk())
            .andReturn();
        long id = objectMapper.readTree(list.getResponse().getContentAsString())
            .get("content").get(0).get("id").asLong();

        mockMvc.perform(get("/api/customers/{id}", id).with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accounts.length()").value(1))
            .andExpect(jsonPath("$.accounts[0].accountNo").value("100000001"));
    }

    @Test
    void nonAdminCannotUseCustomerEndpoints() throws Exception {
        mockMvc.perform(get("/api/customers").with(customerJwt("batbold")))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/customers")
                .with(customerJwt("batbold"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\": \"X\", \"lastName\": \"Y\"}"))
            .andExpect(status().isForbidden());
    }
}
