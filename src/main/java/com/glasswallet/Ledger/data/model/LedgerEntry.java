package com.glasswallet.Ledger.data.model;

import com.glasswallet.Ledger.enums.LedgerType;
import com.glasswallet.Ledger.enums.Status;
import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.user.data.models.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String companyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User userId;

    @Enumerated(EnumType.STRING)
    private LedgerType type;

    private BigDecimal amount;

    private String currency;

    private String reference;

    private String senderId;

    private String receiverId;

    @Enumerated(EnumType.STRING)
    private Status status;

    private BigDecimal balanceCrypto;

    private BigDecimal balanceUsd;

    private Instant timestamp;

    private String platformId;

    private String platformUserId; // Added to match logWithdrawal usage

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = true)
    private Wallet wallet;
}