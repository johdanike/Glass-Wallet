package com.glasswallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.glasswallet.user",
        "com.glasswallet.security",
        "com.glasswallet.Wallet",
        "com.glasswallet.transaction"
})
@EnableScheduling
public class GlassWalletApplication {
    public static void main(String[] args) {
        SpringApplication.run(GlassWalletApplication.class, args);
    }
}
