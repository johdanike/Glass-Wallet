package com.glasswallet.fiat.data.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
@Data
@Configuration
@ConfigurationProperties(prefix = "paystack")
@Getter
@Setter

public class PayStackData {

    private String secret;
    private String baseUrl;


}
