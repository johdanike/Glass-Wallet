package com.glasswallet.fiat.dtos.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Getter
@Setter

public class PayStackRequest {
    private String email;
    private BigDecimal amount;

}
