package mn.golomt.deposit.deposit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DepositProductTests {

    @Autowired
    private MockMvc mockMvc;

    private static RequestPostProcessor customerJwt() {
        return jwt()
            .jwt(token -> token.subject("batbold").claim("displayName", "Bat Bold").claim("role", "VIEWER"))
            .authorities(new SimpleGrantedAuthority("ROLE_VIEWER"));
    }

    @Test
    void productsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/deposit-products"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void productsReturnConfiguredTermsAndBounds() throws Exception {
        mockMvc.perform(get("/api/deposit-products").with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].termMonths").value(3))
            .andExpect(jsonPath("$[0].annualRatePercent").value(8.0))
            .andExpect(jsonPath("$[1].termMonths").value(6))
            .andExpect(jsonPath("$[1].annualRatePercent").value(10.0))
            .andExpect(jsonPath("$[2].termMonths").value(12))
            .andExpect(jsonPath("$[2].annualRatePercent").value(12.5))
            .andExpect(jsonPath("$[0].minAmount").value(100000.00))
            .andExpect(jsonPath("$[0].maxAmount").value(3000000.00));
    }
}
