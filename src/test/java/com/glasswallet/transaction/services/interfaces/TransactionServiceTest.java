package com.glasswallet.transaction.services.interfaces;


import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.Wallet.enums.WalletType;
import com.glasswallet.platform.data.models.PlatformUser;
import com.glasswallet.platform.data.repositories.PlatformUserRepository;
import com.glasswallet.transaction.data.models.Transaction;
import com.glasswallet.transaction.enums.TransactionStatus;
import com.glasswallet.transaction.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class TransactionServiceTest {
    private PlatformUserRepository userRepo;
    private LedgerEntry ledger;
    private Bank bank;
    private PaymentGateway paymentGateway;
    private OnRampProtocol onRamp;
    private OffRampProtocol offRamp;
    private Wallet wallet;

    UUID senderId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();
    UUID companyId = UUID.randomUUID();

    PlatformUser sender, receiver;

    TransactionServiceTest(Wallet wallet) {
        this.wallet = wallet;
    }


    @BeforeEach
    void setup() {
        userRepo = mock(PlatformUserRepository.class);
        ledger = mock(LedgerEntry.class);
        bank = mock(Bank.class);
        paymentGateway = mock(PaymentGateway.class);
        onRamp = mock(OnRampProtocol.class);
        offRamp = mock(OffRampProtocol.class);

        wallet = new Wallet();
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setWalletAddress(wallet.getWalletAddress());
        wallet.setAccountNumber("1234567890");
        wallet.setWalletType(WalletType.FIAT);

        sender = new PlatformUser();
        sender.setPlatformId(String.valueOf(senderId));
        sender.setBalanceFiat(BigDecimal.valueOf(1000));
        receiver = new PlatformUser();
        receiver.setPlatformId(String.valueOf(receiverId));
        receiver.setBalanceFiat(BigDecimal.valueOf(500));

        when(userRepo.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepo.findById(receiverId)).thenReturn(Optional.of(receiver));
    }

//    @Test
//    void transact_transferFiat_success() {
//        Transaction tx = wallet.transact(senderId, receiverId, companyId, TransactionType.TRANSFER, "internal", BigDecimal.valueOf(100));
//
//        assertNotNull(tx);
//        assertEquals(TransactionStatus.PENDING, tx.getStatus());
//        assertEquals(BigDecimal.valueOf(900), sender.getBalanceFiat());
//        assertEquals(BigDecimal.valueOf(600), receiver.getBalanceFiat());
//
//        verify(ledger).logTransaction(tx);
//    }

}