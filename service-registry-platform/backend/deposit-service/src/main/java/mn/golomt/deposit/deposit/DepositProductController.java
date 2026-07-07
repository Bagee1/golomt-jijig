package mn.golomt.deposit.deposit;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mn.golomt.deposit.config.DepositProperties;
import mn.golomt.deposit.deposit.dto.DepositProductResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deposit-products")
@RequiredArgsConstructor
public class DepositProductController {

    private final DepositProperties properties;

    @GetMapping
    public List<DepositProductResponse> list() {
        return properties.products()
            .stream()
            .map(product -> new DepositProductResponse(
                product.termMonths(),
                product.annualRatePercent(),
                properties.minAmount(),
                properties.maxAmount()
            ))
            .toList();
    }
}
