package com.glasswallet.transaction.data.models;

import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.transaction.enums.CurrencyType;
import com.glasswallet.transaction.enums.TransactionStatus;
import com.glasswallet.transaction.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@RequiredArgsConstructor
@Table(name = "transactions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "platform_id", "transaction_Id"})
})
@Builder
@AllArgsConstructor
public class Transaction {
    @Id
    @Column(name = "transaction_Id")
    private UUID id;

    @JoinColumn(name = "user_id", nullable = false)
    private String senderId;

    @Column(name = "receiver_id",   nullable = false)
    private String receiverId;

    @JoinColumn(name = "platform_id", nullable = false)
    private String platformId;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(name = "amount",  nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private WalletCurrency currency;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private Instant timestamp;

    private boolean onChain;
    private String suiTxHash;
    private BigDecimal gasFee;
    private String direction;
}
