package com.glasswallet.transaction.dtos.request;

import com.glasswallet.user.data.models.User;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Data
@Getter
@Setter
public class BulkDisbursementRequest {
    private List<TransferRequest> disbursements;
    private User userId;
    private String senderId;
    private String receiverId;
    private BigDecimal amount;
    private String currency;
    private String reference;
    private String companyId;

    public BigDecimal getTotalAmount() {
        return amount;
    }
}
