package com.glasswallet.Ledger.data.model;

import com.glasswallet.Ledger.enums.LedgerType;
import com.glasswallet.Ledger.enums.Status;
import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.transaction.data.models.Transaction;
import com.glasswallet.user.data.models.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LedgerEntry {

    @jakarta.persistence.Id
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String companyId;
    @ManyToOne
    @JoinColumn(name = "user_id_id")
    private  User userId;
    @Enumerated(EnumType.STRING)
    private LedgerType type;
    private BigDecimal amount;
    private String currency;
    private String reference;
    private String senderId;
    private String receiverId;
    private Status status;
    private String balanceCrypto;
    private String balanceUsd;
    private Instant timestamp;
    @ManyToOne
    private Wallet wallet;

}
