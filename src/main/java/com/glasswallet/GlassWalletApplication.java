package com.glasswallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EntityScan(basePackages = "com.glasswallet")
@EnableJpaRepositories(basePackages = "com.glasswallet")

public class GlassWalletApplication {
    public static void main(String[] args) {
        SpringApplication.run(GlassWalletApplication.class, args);
    }
}
