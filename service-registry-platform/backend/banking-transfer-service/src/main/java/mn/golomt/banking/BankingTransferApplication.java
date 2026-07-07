package mn.golomt.banking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BankingTransferApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingTransferApplication.class, args);
    }
}
