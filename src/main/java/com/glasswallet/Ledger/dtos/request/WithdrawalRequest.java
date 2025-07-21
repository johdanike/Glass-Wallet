package com.glasswallet.Ledger.dtos.request;

import com.glasswallet.user.data.models.User;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.CloseableThreadContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Getter
@Setter
public class WithdrawalRequest {
    private User userId;
    private String senderId;
    private BigDecimal amount;
    private String currency;
    private String receiverId;
    private String companyId;
    private String reference;
}
