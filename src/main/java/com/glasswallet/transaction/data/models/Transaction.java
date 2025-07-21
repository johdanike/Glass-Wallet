package com.glasswallet.transaction.data.models;

import com.glasswallet.transaction.enums.CurrencyType;
import com.glasswallet.transaction.enums.TransactionStatus;
import com.glasswallet.transaction.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;

@Entity
@Data
public class Transaction {
    @Id
    @Column(name = "transaction_Id")
    private Long id;

    @JoinColumn(name = "user_id", nullable = false)
    private String userId;

    @JoinColumn(name = "platform_id", nullable = false)
    private String platformId;
    private TransactionType transactionType;
    @Column(name = "amount",  nullable = false)
    private double amount;
    private CurrencyType currency;
    @Column(name = "sender_id",  nullable = false)
    private String senderId;
    @Column(name = "receiver_id",   nullable = false)
    private String receiverId;
    private TransactionStatus status;
    private LocalDateTime timestamp;


}
