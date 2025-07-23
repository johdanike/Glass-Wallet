//package com.glasswallet.Wallet.service.interfaces;
//
//import com.glasswallet.Ledger.service.interfaces.LedgerService;
//import com.glasswallet.Wallet.data.model.Wallet;
//import com.glasswallet.Wallet.data.repositories.WalletRepository;
//import com.glasswallet.Wallet.enums.WalletCurrency;
//import com.glasswallet.Wallet.enums.WalletStatus;
//import com.glasswallet.Wallet.enums.WalletType;
//import com.glasswallet.Wallet.exceptions.InsufficientBalanceException;
//import com.glasswallet.transaction.data.repositories.TransactionRepository;
//import com.glasswallet.transaction.enums.TransactionType;
//import com.glasswallet.transaction.services.interfaces.SuiRateService;
//import com.glasswallet.transaction.services.interfaces.TransactionService;
//import com.glasswallet.user.data.models.User;
//import com.glasswallet.user.data.repositories.UserRepository;
//import com.glasswallet.Wallet.service.implementation.WalletServiceImpl;
//import com.glasswallet.transaction.data.models.Transaction;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.math.BigDecimal;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//public class WalletServiceImplTest {
//
//    private WalletServiceImpl walletService;
//
//    private UserRepository userRepository;
//    private WalletRepository walletRepository;
//    private SuiRateService suiRateService;
//    private LedgerService ledgerService;
//    private TransactionRepository transactionRepository;
//
//    private UUID senderId = UUID.randomUUID();
//    private UUID receiverId = UUID.randomUUID();
//    private UUID companyId = UUID.randomUUID();
//
//    private User sender;
//    private User receiver;
//    private Wallet senderWallet;
//    private Wallet receiverWallet;
//
//    @BeforeEach
//    void setUp() {
//        userRepository = mock(UserRepository.class);
//        walletRepository = mock(WalletRepository.class);
//        suiRateService = mock(SuiRateService.class);
//        LedgerService ledgerService = mock(LedgerService.class);
//
//        walletService = new WalletServiceImpl(
//                userRepository, walletRepository,
//                suiRateService, null,
//                null,
//                ledgerService, transactionRepository
//        );
//
//        sender = new User();
//        sender.setId(senderId);
//        receiver = new User();
//        receiver.setId(receiverId);
//
//        senderWallet = new Wallet();
//        senderWallet.setUser(sender);
//        senderWallet.setCurrencyType(WalletCurrency.NGN);
//        senderWallet.setWalletType(WalletType.FIAT);
//        senderWallet.setStatus(WalletStatus.ACTIVE);
//        senderWallet.setBalance(BigDecimal.valueOf(1000));
//
//        receiverWallet = new Wallet();
//        receiverWallet.setUser(receiver);
//        receiverWallet.setCurrencyType(WalletCurrency.NGN);
//        receiverWallet.setWalletType(WalletType.FIAT);
//        receiverWallet.setStatus(WalletStatus.ACTIVE);
//        receiverWallet.setBalance(BigDecimal.valueOf(500));
//
//        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
//        when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
//        when(walletRepository.findByUserAndCurrencyType(sender, WalletCurrency.NGN))
//                .thenReturn(Optional.of(senderWallet));
//        when(walletRepository.findByUserAndCurrencyType(receiver, WalletCurrency.NGN))
//                .thenReturn(Optional.of(receiverWallet));
//    }
//
//    @Test
//    void testTransact_successfulTransfer() {
//        Transaction tx = walletService.transact(
//                senderId,
//                receiverId,
//                companyId,
//                TransactionType.FIAT_TRANSFER,
//                WalletCurrency.NGN,
//                "ref123",
//                BigDecimal.valueOf(100)
//        );
//
//        assertNotNull(tx);
//        assertEquals(senderId.toString(), tx.getSenderId());
//        assertEquals(receiverId.toString(), tx.getReceiverId());
//        assertEquals(BigDecimal.valueOf(900), senderWallet.getBalance());
//        assertEquals(BigDecimal.valueOf(600), receiverWallet.getBalance());
//
//        verify(walletRepository, times(1)).save(senderWallet);
//        verify(walletRepository, times(1)).save(receiverWallet);
//    }
//
//    @Test
//    void testTransact_insufficientBalance_throwsException() {
//        senderWallet.setBalance(BigDecimal.valueOf(50));
//
//        assertThrows(InsufficientBalanceException.class, () ->
//                walletService.transact(
//                        senderId,
//                        receiverId,
//                        companyId,
//                        TransactionType.FIAT_TRANSFER,
//                        WalletCurrency.NGN,
//                        "ref123",
//                        BigDecimal.valueOf(100)
//                )
//        );
//    }
//}
