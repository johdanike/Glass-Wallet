package com.glasswallet.transaction.dtos.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class PayStackRecipientRequest {
    private String type = "nuban";
    private String name;
    private String account_number;
    private String bank_code;
    private String currency = "NGN";
}
