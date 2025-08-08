package com.glasswallet.fiat.dtos.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class PayStackTransferRequest {
    private String source = "balance";
    private Integer amount;
    private String recipient;
    private String reason;
}
