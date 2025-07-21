package com.glasswallet.Ledger.dtos.request;

import com.glasswallet.user.data.models.User;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Getter
@Setter
@AllArgsConstructor

public class DepositRequest {
    private User userId;
    private String senderId;
    private String companyId;
    private String receiverId;
    private BigDecimal amount;
    private String currency;
    private String reference;


}
