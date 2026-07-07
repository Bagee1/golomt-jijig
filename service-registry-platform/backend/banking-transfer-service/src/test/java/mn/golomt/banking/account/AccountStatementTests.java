package mn.golomt.banking.account;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
class AccountStatementTests {

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
    void statementShowsOpeningBalanceMovementsAndClosingBalance() throws Exception {
        createTransfer("100000001", "100000002", 50000);

        // Opening balance must include the seeded 1,000,000 even though the seed has
        // no ledger entries behind it.
        mockMvc.perform(get("/api/accounts/{accountNo}/statement", "100000001").with(customerJwt("batbold")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountNo").value("100000001"))
            .andExpect(jsonPath("$.openingBalance").value(1000000.00))
            .andExpect(jsonPath("$.closingBalance").value(950000.00))
            .andExpect(jsonPath("$.totalDebit").value(50000.00))
            .andExpect(jsonPath("$.totalCredit").value(0))
            .andExpect(jsonPath("$.entries.totalElements").value(1))
            .andExpect(jsonPath("$.entries.content[0].entryType").value("DEBIT"))
            .andExpect(jsonPath("$.entries.content[0].balanceAfter").value(950000.00))
            .andExpect(jsonPath("$.entries.content[0].counterpartyAccountNo").value("100000002"));

        mockMvc.perform(get("/api/accounts/{accountNo}/statement", "100000002").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openingBalance").value(500000.00))
            .andExpect(jsonPath("$.closingBalance").value(550000.00))
            .andExpect(jsonPath("$.totalCredit").value(50000.00))
            .andExpect(jsonPath("$.entries.content[0].counterpartyAccountNo").value("100000001"));
    }

    @Test
    void statementDateFilterExcludesOutOfRangeEntries() throws Exception {
        createTransfer("100000001", "100000002", 50000);

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        mockMvc.perform(get("/api/accounts/{accountNo}/statement", "100000001")
                .with(customerJwt("batbold"))
                .param("from", tomorrow.toString())
                .param("to", tomorrow.plusDays(1).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entries.totalElements").value(0))
            // No movements from tomorrow onwards: opening equals the current balance.
            .andExpect(jsonPath("$.openingBalance").value(950000.00))
            .andExpect(jsonPath("$.closingBalance").value(950000.00));
    }

    @Test
    void statementRejectsInvertedDateRange() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountNo}/statement", "100000001")
                .with(adminJwt())
                .param("from", "2026-07-10")
                .param("to", "2026-07-01"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void customerCannotReadForeignStatement() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountNo}/statement", "100000002").with(customerJwt("batbold")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN_ACCOUNT"));
    }

    private void createTransfer(String fromAccountNo, String toAccountNo, int amount) throws Exception {
        mockMvc.perform(post("/api/transfers")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fromAccountNo": "%s",
                      "toAccountNo": "%s",
                      "amount": %d,
                      "description": "Statement test transfer"
                    }
                    """.formatted(fromAccountNo, toAccountNo, amount)))
            .andExpect(status().isCreated());
    }
}
