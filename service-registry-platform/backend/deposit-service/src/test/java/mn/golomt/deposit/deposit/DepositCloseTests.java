package mn.golomt.deposit.deposit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import mn.golomt.deposit.client.BankTransferCommand;
import mn.golomt.deposit.client.BankTransferResult;
import mn.golomt.deposit.client.BankingClient;
import mn.golomt.deposit.client.BankingUnauthorizedException;
import mn.golomt.deposit.client.BankingUnavailableException;
import mn.golomt.deposit.client.ServiceTokenProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// MERGE so each method's seed @Sql runs IN ADDITION TO the class-level cleanup
// (default OVERRIDE would drop the delete and pile up rows -> PK clash).
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
    statements = {"delete from deposit_audit_logs", "delete from deposits"},
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class DepositCloseTests {

    private static final long DEPOSIT_ID = 700L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BankingClient bankingClient;

    @MockitoBean
    private ServiceTokenProvider serviceTokenProvider;

    private static RequestPostProcessor customerJwt() {
        return jwt()
            .jwt(token -> token.tokenValue("customer-token").subject("batbold")
                .claim("displayName", "Bat Bold").claim("role", "VIEWER"))
            .authorities(new SimpleGrantedAuthority("ROLE_VIEWER"));
    }

    private static RequestPostProcessor otherCustomerJwt() {
        return jwt()
            .jwt(token -> token.subject("sarnai").claim("role", "VIEWER"))
            .authorities(new SimpleGrantedAuthority("ROLE_VIEWER"));
    }

    // 1,000,000 @ 12.5% over exactly 365 days (2025-07-07 -> 2026-07-07) = 125,000.00 interest
    private static final String SEED_MATURED = "insert into deposits "
        + "(id, deposit_no, customer_username, linked_account_no, principal, annual_rate, term_months, opened_at, maturity_date, status) "
        + "values (700, 'DEP-7001', 'batbold', '100000001', 1000000.00, 12.5, 12, '2025-07-07T00:00:00Z', '2026-07-07', 'OPEN')";

    // maturity far in the future -> early close, interest forfeited
    private static final String SEED_EARLY = "insert into deposits "
        + "(id, deposit_no, customer_username, linked_account_no, principal, annual_rate, term_months, opened_at, maturity_date, status) "
        + "values (700, 'DEP-7002', 'batbold', '100000001', 500000.00, 10.0, 12, '2026-07-07T00:00:00Z', '2027-07-07', 'OPEN')";

    @Test
    @Sql(statements = SEED_MATURED, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void maturedCloseePaysPrincipalPlusInterestFromSettlementWithServiceToken() throws Exception {
        when(serviceTokenProvider.token()).thenReturn("svc-token");
        when(bankingClient.transfer(any(), any(), any()))
            .thenReturn(new BankTransferResult(50L, "TRF-PAYOUT", "SUCCESS"));

        mockMvc.perform(post("/api/deposits/{id}/close", DEPOSIT_ID).with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOSED"))
            .andExpect(jsonPath("$.closeType").value("MATURED"))
            .andExpect(jsonPath("$.interestAmount").value(125000.00))
            .andExpect(jsonPath("$.payoutAmount").value(1125000.00))
            .andExpect(jsonPath("$.payoutTransferRef").value("TRF-PAYOUT"));

        ArgumentCaptor<BankTransferCommand> commandCaptor = ArgumentCaptor.forClass(BankTransferCommand.class);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(bankingClient).transfer(commandCaptor.capture(), tokenCaptor.capture(), keyCaptor.capture());

        assertThat(commandCaptor.getValue().fromAccountNo()).isEqualTo("900000001");
        assertThat(commandCaptor.getValue().toAccountNo()).isEqualTo("100000001");
        assertThat(tokenCaptor.getValue()).isEqualTo("svc-token"); // service token, not customer-token
        assertThat(keyCaptor.getValue()).isEqualTo("dep-DEP-7001-payout");
    }

    @Test
    @Sql(statements = SEED_EARLY, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void earlyCloseForfeitsInterestAndPaysOnlyPrincipal() throws Exception {
        when(serviceTokenProvider.token()).thenReturn("svc-token");
        when(bankingClient.transfer(any(), any(), any()))
            .thenReturn(new BankTransferResult(51L, "TRF-EARLY", "SUCCESS"));

        mockMvc.perform(post("/api/deposits/{id}/close", DEPOSIT_ID).with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOSED_EARLY"))
            .andExpect(jsonPath("$.closeType").value("EARLY"))
            .andExpect(jsonPath("$.interestAmount").value(0.00))
            .andExpect(jsonPath("$.payoutAmount").value(500000.00));
    }

    @Test
    @Sql(statements = SEED_MATURED, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void payoutUnavailableKeepsPayoutPendingThenRetryCloses() throws Exception {
        when(serviceTokenProvider.token()).thenReturn("svc-token");
        when(bankingClient.transfer(any(), any(), any()))
            .thenThrow(new BankingUnavailableException("banking down"))
            .thenReturn(new BankTransferResult(52L, "TRF-RETRY", "SUCCESS"));

        mockMvc.perform(post("/api/deposits/{id}/close", DEPOSIT_ID).with(customerJwt()))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.code").value("BANKING_UNAVAILABLE"));

        mockMvc.perform(get("/api/deposits/{id}", DEPOSIT_ID).with(customerJwt()))
            .andExpect(jsonPath("$.status").value("PAYOUT_PENDING"))
            .andExpect(jsonPath("$.interestAmount").value(125000.00)); // amounts already persisted

        mockMvc.perform(post("/api/deposits/{id}/close", DEPOSIT_ID).with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOSED"))
            .andExpect(jsonPath("$.payoutAmount").value(1125000.00));
    }

    @Test
    @Sql(statements = SEED_MATURED, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void staleServiceTokenIsRefreshedAndRetriedOnce() throws Exception {
        when(serviceTokenProvider.token()).thenReturn("stale", "fresh");
        when(bankingClient.transfer(any(), any(), any()))
            .thenThrow(new BankingUnauthorizedException("401"))
            .thenReturn(new BankTransferResult(53L, "TRF-REFRESH", "SUCCESS"));

        mockMvc.perform(post("/api/deposits/{id}/close", DEPOSIT_ID).with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOSED"));

        verify(serviceTokenProvider).invalidate();
        verify(bankingClient, times(2)).transfer(any(), any(), eq("dep-DEP-7001-payout"));
    }

    @Test
    @Sql(statements = SEED_MATURED, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void doubleCloseReturnsConflict() throws Exception {
        when(serviceTokenProvider.token()).thenReturn("svc-token");
        when(bankingClient.transfer(any(), any(), any()))
            .thenReturn(new BankTransferResult(54L, "TRF-ONCE", "SUCCESS"));

        mockMvc.perform(post("/api/deposits/{id}/close", DEPOSIT_ID).with(customerJwt()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/deposits/{id}/close", DEPOSIT_ID).with(customerJwt()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DEPOSIT_ALREADY_CLOSED"));
    }

    @Test
    @Sql(statements = SEED_MATURED, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void foreignUserCannotClose() throws Exception {
        mockMvc.perform(post("/api/deposits/{id}/close", DEPOSIT_ID).with(otherCustomerJwt()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
