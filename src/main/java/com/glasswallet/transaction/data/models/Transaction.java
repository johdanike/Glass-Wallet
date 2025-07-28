package com.glasswallet.transaction.data.models;

import com.glasswallet.transaction.enums.TransactionStatus;
import com.glasswallet.transaction.enums.TransactionType;
import com.glasswallet.Wallet.enums.WalletCurrency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sender_id")
    private String senderId;

    @Column(name = "receiver_id")
    private String receiverId;

    @Column(name = "platform_id")
    private String platformId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency")
    private WalletCurrency currency;

    @Column(name = "amount")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TransactionStatus status;

    @Column(name = "timestamp")
    private Instant timestamp;

    @Column(name = "gas_fee")
    private BigDecimal gasFee;

    @Column(name = "on_chain")
    private boolean onChain;

    @Column(name = "external_wallet_address")
    private String externalWalletAddress;
}