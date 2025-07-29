package com.glasswallet.Wallet.service.implementation;

import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.Wallet.data.repositories.WalletRepository;
import com.glasswallet.Wallet.dtos.requests.CreateWalletRequest;
import com.glasswallet.Wallet.dtos.response.CreateWalletResponse;
import com.glasswallet.Wallet.dtos.response.WalletBalanceResponse;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.Wallet.enums.WalletStatus;
import com.glasswallet.Wallet.enums.WalletType;
import com.glasswallet.Wallet.exceptions.WalletNotFoundException;
import com.glasswallet.Wallet.service.interfaces.WalletResolver;
import com.glasswallet.Wallet.service.interfaces.WalletService;
import com.glasswallet.Wallet.utils.PaymentResult;
import com.glasswallet.Wallet.utils.WalletUtils;
import com.glasswallet.transaction.dtos.request.DepositRequest;
import com.glasswallet.transaction.dtos.request.TransferRequest;
import com.glasswallet.transaction.dtos.request.WithdrawalRequest;
import com.glasswallet.transaction.dtos.response.DepositResponse;
import com.glasswallet.transaction.dtos.response.TransferResponse;
import com.glasswallet.transaction.dtos.response.WithdrawalResponse;
import com.glasswallet.transaction.services.interfaces.SuiRateService;
import com.glasswallet.transaction.services.interfaces.TransactionService;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.data.repositories.UserRepository;
import com.glasswallet.user.dtos.responses.WalletProfileDto;
import com.glasswallet.user.exceptions.UserNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final SuiRateService suiRateService;
    private final WalletResolver walletResolver;
    private final PasswordEncoder passwordEncoder;
//    private final LedgerService ledgerService;
//    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    @Value("${wallet_address}")
    private String defaultSuiWalletAddress;


    @Override
    public CreateWalletResponse createWalletForUser(UUID userId, CreateWalletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        createWalletIfNotExists(user);

        WalletCurrency currency = WalletUtils.resolveCurrencyFromPhoneNumber(user.getPhoneNumber());
        Wallet wallet = walletRepository.findByUserAndCurrencyType(user, currency)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        return mapToCreateWalletResponse(wallet);
    }
    private CreateWalletResponse mapToCreateWalletResponse(Wallet wallet) {
        return CreateWalletResponse.builder()
                .message("Wallet created successfully")
                .accountNumber(wallet.getAccountNumber())
                .walletCurrency(wallet.getCurrencyType())
                .walletType(wallet.getWalletType())
                .walletStatus(wallet.getStatus())
                .balance(wallet.getBalance())
                .build();
    }

    @Override
    public void createWalletIfNotExists(User user) {
        if (!walletRepository.existsByUserAndCurrencyType(user, WalletCurrency.NGN)) {
            createFiatWallet(user);
        }

        if (!walletRepository.existsByUserAndCurrencyType(user, WalletCurrency.SUI)) {
            createSuiWallet(user);
        }
    }

    private void createFiatWallet(User user) {
        Wallet fiatWallet = new Wallet();
        fiatWallet.setUser(user);
        fiatWallet.setCurrencyType(WalletCurrency.NGN);
        fiatWallet.setAccountNumber(WalletUtils.generateAccountNumberFromPhoneNumber(user.getPhoneNumber()));
        fiatWallet.setBalance(BigDecimal.ZERO);
        fiatWallet.setStatus(WalletStatus.ACTIVE);
        walletRepository.save(fiatWallet);
    }

    @Override
    public CreateWalletResponse createWallet(User user) {
        createWalletIfNotExists(user);
        Wallet fiatWallet = walletRepository.findByUserAndCurrencyType(user, WalletCurrency.NGN)
                .orElseThrow(() -> new WalletNotFoundException("Fiat wallet not found"));
        return mapToCreateWalletResponse(fiatWallet);
    }

    @Override
    public DepositResponse depositFiat(UUID receiverId, UUID companyId, BigDecimal amount, String reference) {
        return transactionService.processDeposit(DepositRequest.builder()
                .receiverId(receiverId)
                .companyId(companyId)
                .currency(WalletCurrency.NGN)
                .amount(amount)
                .reference(reference)
                .build());
    }

    @Override
    public DepositResponse depositSui(UUID receiverId, UUID companyId, BigDecimal amount, String reference) {
        return transactionService.processDeposit(DepositRequest.builder()
                .receiverId(receiverId)
                .companyId(companyId)
                .currency(WalletCurrency.SUI)
                .amount(amount)
                .reference(reference)
                .build());
    }

    @Override
    public WithdrawalResponse withdrawFiat(UUID senderId, UUID companyId, BigDecimal amount, String reference) {
        return transactionService.processWithdrawal(WithdrawalRequest.builder()
                .senderId(senderId.toString())
                .companyId(String.valueOf(companyId))
                .currency(WalletCurrency.NGN.name())
                .amount(amount)
                .reference(reference)
                .build());
    }

    @Override
    public WithdrawalResponse withdrawSui(UUID senderId, UUID companyId, BigDecimal amount, String reference) {
        return transactionService.processWithdrawal(WithdrawalRequest.builder()
                .senderId(senderId.toString())
                .companyId(String.valueOf(companyId))
                .currency(WalletCurrency.SUI.name())
                .amount(amount)
                .reference(reference)
                .build());
    }

    @Override
    public TransferResponse transfer(UUID senderId, UUID receiverId, UUID companyId, BigDecimal amount, WalletCurrency currency, String reference) {
        return transactionService.processTransfer(TransferRequest.builder()
                .senderId(senderId.toString())
                .receiverId(receiverId.toString())
                .companyId(companyId.toString())
                .amount(amount)
                .currency(currency.name())
                .reference(reference)
                .build());
    }


    private void verifyPassword(User user, String password) {
        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }
    }

    @Override
    public WalletBalanceResponse getUserWalletBalances(String recipientIdentifier, String password) {
        Wallet fiat = walletResolver.resolveWallet(recipientIdentifier, WalletCurrency.NGN)
                .orElseThrow(() -> new WalletNotFoundException("Fiat wallet not found"));
        Wallet sui = walletResolver.resolveWallet(recipientIdentifier, WalletCurrency.SUI)
                .orElseThrow(() -> new WalletNotFoundException("SUI wallet not found"));

        verifyPassword(fiat.getUser(), password);
        BigDecimal exchangeRate = suiRateService.getSuiToNgnRate();

        return WalletBalanceResponse.builder()
                .fiatBalance(fiat.getBalance())
                .fiatCurrency("NGN")
                .fiatEquivalentOfSui(fiat.getBalance().divide(exchangeRate, 2, RoundingMode.HALF_UP))
                .suiBalance(sui.getBalance())
                .suiToken("SUI")
                .suiEquivalentOfFiat(sui.getBalance().multiply(exchangeRate))
                .suiToNgnRate(exchangeRate)
                .build();
    }

    @Override
    public List<Wallet> getWallets(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return walletRepository.findByUser(user);
    }

    @Override
    @Transactional
    public PaymentResult receivePayment(String recipientIdentifier, WalletCurrency currency, BigDecimal amount) {
        validateAmount(amount);

        Wallet wallet = resolveAndValidateWallet(recipientIdentifier, currency);

        buildAndCreateDepositRequest(currency, amount, wallet);

        return new PaymentResult(wallet.getId(), currency, amount);
    }


    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }


    private Wallet resolveAndValidateWallet(String recipientIdentifier, WalletCurrency currency) {
        Wallet wallet = walletResolver.resolveWallet(recipientIdentifier, currency)
                .orElseThrow(() -> new WalletNotFoundException("Recipient wallet not found"));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Recipient wallet is inactive");
        }

        return wallet;
    }


    private void buildAndCreateDepositRequest(WalletCurrency currency, BigDecimal amount, Wallet wallet) {
        DepositRequest request = DepositRequest.builder()
                .receiverId(wallet.getUser().getId())
                .companyId(UUID.fromString(wallet.getUser().getPlatformId()))
                .currency(currency)
                .amount(amount)
                .reference(String.valueOf(wallet.getId()))
                .build();

        transactionService.processDeposit(request);
    }


    @Override
    public WalletProfileDto getProfile(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        List<Wallet> wallets = walletRepository.findByUser(user);
        if (wallets.isEmpty()) {
            throw new WalletNotFoundException("No wallets found for this user");
        }

        return WalletProfileDto.builder()
                .userId(String.valueOf(user.getId()))
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .wallets(wallets)
                .build();
    }

    @Override
    public Wallet getWalletById(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));
    }

    private void createSuiWallet(User user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setWalletType(WalletType.CRYPTO);
        wallet.setCurrencyType(WalletCurrency.SUI);
        wallet.setWalletAddress(defaultSuiWalletAddress);
        wallet.setTokenSymbol("SUI");
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.ACTIVE);
        walletRepository.save(wallet);
    }


}







