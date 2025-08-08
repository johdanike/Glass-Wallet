package com.glasswallet.Wallet.service.interfaces;

import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.Wallet.data.repositories.WalletRepository;
import com.glasswallet.Wallet.dtos.requests.CreateWalletRequest;
import com.glasswallet.Wallet.dtos.response.CreateWalletResponse;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.Wallet.enums.WalletStatus;
import com.glasswallet.Wallet.exceptions.WalletNotFoundException;
import com.glasswallet.Wallet.service.implementation.WalletServiceImpl;
import com.glasswallet.transaction.dtos.request.DepositRequest;
import com.glasswallet.transaction.dtos.response.DepositResponse;
import com.glasswallet.transaction.services.interfaces.SuiRateService;
import com.glasswallet.transaction.services.interfaces.TransactionService;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.data.repositories.UserRepository;
import com.glasswallet.user.exceptions.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private SuiRateService suiRateService;
    @Mock private WalletResolver walletResolver;
    @Mock private TransactionService transactionService;

    @InjectMocks private WalletServiceImpl walletService;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setPhoneNumber("+2348012345678");
        user.setPlatformId(UUID.randomUUID().toString());

        wallet = new Wallet();
        wallet.setUser(user);
        wallet.setCurrencyType(WalletCurrency.NGN);
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setBalance(BigDecimal.valueOf(1000));
    }

    @Test
    void testCreateWalletForUser_userNotFound_throwsException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                walletService.createWalletForUser(userId, new CreateWalletRequest()));
    }

    @Test
    void testCreateWalletForUser_returnsResponse() {
        UUID userId = user.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(walletRepository.existsByUserAndCurrencyType(user, WalletCurrency.NGN)).thenReturn(false);
        when(walletRepository.existsByUserAndCurrencyType(user, WalletCurrency.SUI)).thenReturn(false);
        when(walletRepository.findByUserAndCurrencyType(user, WalletCurrency.NGN)).thenReturn(Optional.of(wallet));

        CreateWalletResponse response = walletService.createWalletForUser(userId, new CreateWalletRequest());

        assertNotNull(response);
        assertEquals(WalletCurrency.NGN, response.getWalletCurrency());
    }

    @Test
    void testDepositFiat_success() {
        UUID receiverId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(1000);
        String ref = "txn123";

        DepositResponse mockResponse = new DepositResponse();
        when(transactionService.processDepositForSui(any(DepositRequest.class))).thenReturn(mockResponse);

        DepositResponse result = walletService.depositFiat(receiverId, companyId, amount, ref);

        assertEquals(mockResponse, result);
        verify(transactionService).processDepositForSui(any(DepositRequest.class));
    }

    @Test
    void testReceivePayment_walletNotFound_throwsException() {
        when(walletResolver.resolveWallet(anyString(), eq(WalletCurrency.NGN))).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () ->
                walletService.receivePayment("user123", WalletCurrency.NGN, BigDecimal.valueOf(100)));
    }

    @Test
    void testReceivePayment_inactiveWallet_throwsException() {
        wallet.setStatus(WalletStatus.PENDING);
        when(walletResolver.resolveWallet(anyString(), eq(WalletCurrency.NGN))).thenReturn(Optional.of(wallet));

        assertThrows(IllegalStateException.class, () ->
                walletService.receivePayment("user123", WalletCurrency.NGN, BigDecimal.valueOf(100)));
    }

    @Test
    void testReceivePayment_invalidAmount_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                walletService.receivePayment("user123", WalletCurrency.NGN, BigDecimal.ZERO));
    }

    @Test
    void testReceivePayment_successful() {
        wallet.setStatus(WalletStatus.ACTIVE);
        when(walletResolver.resolveWallet(anyString(), eq(WalletCurrency.NGN))).thenReturn(Optional.of(wallet));

        DepositResponse mockDeposit = new DepositResponse();
        when(transactionService.processDepositForSui(any())).thenReturn(mockDeposit);

        var result = walletService.receivePayment("user123", WalletCurrency.NGN, BigDecimal.valueOf(1500));

        assertEquals(wallet.getId(), result.getRecipientId());
        assertEquals(WalletCurrency.NGN, result.getCurrency());
        assertEquals(BigDecimal.valueOf(1500), result.getAmount());
    }
}
