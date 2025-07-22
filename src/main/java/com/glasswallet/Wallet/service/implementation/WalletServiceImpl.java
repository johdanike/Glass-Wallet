package com.glasswallet.Wallet.service.implementation;

import com.glasswallet.Ledger.service.interfaces.LedgerService;
import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.Wallet.data.repositories.WalletRepository;
import com.glasswallet.Wallet.dtos.requests.CreateWalletRequest;
import com.glasswallet.Wallet.dtos.response.CreateWalletResponse;
import com.glasswallet.Wallet.dtos.response.WalletBalanceResponse;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.Wallet.enums.WalletStatus;
import com.glasswallet.Wallet.enums.WalletType;
import com.glasswallet.Wallet.exceptions.InsufficientBalanceException;
import com.glasswallet.Wallet.exceptions.WalletNotFoundException;
import com.glasswallet.Wallet.service.interfaces.WalletResolver;
import com.glasswallet.Wallet.service.interfaces.WalletService;
import com.glasswallet.Wallet.utils.PaymentResult;
import com.glasswallet.Wallet.utils.WalletUtils;
import com.glasswallet.transaction.data.models.Transaction;
import com.glasswallet.transaction.data.repositories.TransactionRepository;
import com.glasswallet.transaction.enums.TransactionStatus;
import com.glasswallet.transaction.enums.TransactionType;
import com.glasswallet.transaction.services.interfaces.SuiRateService;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.data.repositories.UserRepository;
import com.glasswallet.user.dtos.responses.WalletProfileDto;
import com.glasswallet.user.exceptions.UserNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
    private final LedgerService ledgerService;
    private final TransactionRepository transactionRepository;

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
    public void depositFiat(String recipientIdentifier, BigDecimal amount) {
        Wallet wallet = walletResolver.resolveWallet(recipientIdentifier, WalletCurrency.NGN)
                .orElseThrow(() -> new WalletNotFoundException("Fiat wallet not found"));
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
    }

    @Override
    public void depositSui(String recipientIdentifier, BigDecimal amount) {
        Wallet wallet = walletResolver.resolveWallet(recipientIdentifier, WalletCurrency.SUI)
                .orElseThrow(() -> new WalletNotFoundException("SUI wallet not found"));
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
    }

    @Override
    public void withdrawFiat(String recipientIdentifier, BigDecimal amount, String password) {
        Wallet wallet = walletResolver.resolveWallet(recipientIdentifier, WalletCurrency.NGN)
                .orElseThrow(() -> new WalletNotFoundException("Fiat wallet not found"));
        verifyPassword(wallet.getUser(), password);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient fiat balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
    }

    private void verifyPassword(User user, String password) {
        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }
    }

    @Override
    public void withdrawSui(String recipientIdentifier, BigDecimal amount, String password) {
        Wallet wallet = walletResolver.resolveWallet(recipientIdentifier, WalletCurrency.SUI)
                .orElseThrow(() -> new WalletNotFoundException("SUI wallet not found"));
        verifyPassword(wallet.getUser(), password);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient SUI balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
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
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        Wallet recipientWallet = walletResolver.resolveWallet(recipientIdentifier, currency)
                .orElseThrow(() -> new WalletNotFoundException("Recipient wallet not found"));

        if (recipientWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Recipient wallet is inactive");
        }

        recipientWallet.setBalance(recipientWallet.getBalance().add(amount));
        walletRepository.save(recipientWallet);

        return new PaymentResult(
                recipientWallet.getId(),
                recipientWallet.getCurrencyType(),
                amount
        );
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
        return null;
    }

    private void createSuiWallet(User user) {
            Wallet wallet = new Wallet();
            wallet.setUser(user);
            wallet.setWalletType(WalletType.CRYPTO);
            wallet.setCurrencyType(WalletCurrency.SUI);
            wallet.setWalletAddress("sui_" + UUID.randomUUID().toString().replace("-", "").substring(0, 32));
            wallet.setTokenSymbol("SUI");
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setStatus(WalletStatus.ACTIVE);
            walletRepository.save(wallet);
    }

    @Transactional
    @Override
    public Transaction transact(UUID senderId, UUID receiverId, UUID companyId, TransactionType type, WalletCurrency currency, String reference, BigDecimal amount) {
        checkIfAmountIsPositive(amount);
        checkTransactionAndCurrencyTypeFromWallet(type, currency);

        fetchSenderAndReceiver foundSenderAndReceiver = getSenderAndReceiver(senderId, receiverId, currency);

        validateBalanceBeforeProcessingTx(amount, foundSenderAndReceiver.senderWallet());

        return processTransaction(companyId, type, amount,
                foundSenderAndReceiver.senderWallet(),
                foundSenderAndReceiver.receiverWallet(),
                foundSenderAndReceiver.sender(),
                foundSenderAndReceiver.receiver());
    }

    private static void checkTransactionAndCurrencyTypeFromWallet(TransactionType type, WalletCurrency currency) {
        if (type == TransactionType.CRYPTO_TRANSFER && currency != WalletCurrency.SUI) {
            throw new IllegalArgumentException("CRYPTO_TRANSFER must use SUI wallet");
        }

        if (type == TransactionType.FIAT_TRANSFER && currency != WalletCurrency.NGN) {
            throw new IllegalArgumentException("FIAT_TRANSFER must use NGN wallet");
        }
    }


    private fetchSenderAndReceiver getSenderAndReceiver(UUID senderId, UUID receiverId, WalletCurrency currency) {
    User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new UserNotFoundException("Sender not found"));
    User receiver = userRepository.findById(receiverId)
            .orElseThrow(() -> new UserNotFoundException("Receiver not found"));

    Wallet senderWallet = walletRepository.findByUserAndCurrencyType(sender, currency)
            .orElseThrow(() -> new WalletNotFoundException("Sender wallet not found"));
    Wallet receiverWallet = walletRepository.findByUserAndCurrencyType(receiver, currency)
            .orElseThrow(() -> new WalletNotFoundException("Receiver wallet not found"));

    return new fetchSenderAndReceiver(sender, receiver, senderWallet, receiverWallet);
}


private record fetchSenderAndReceiver(User sender, User receiver, Wallet senderWallet, Wallet receiverWallet) {
    }

    private static void checkIfAmountIsPositive(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private static void validateBalanceBeforeProcessingTx(BigDecimal amount, Wallet senderWallet) {
        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
    }

    private Transaction processTransaction(UUID platformId, TransactionType type,
                                           BigDecimal amount, Wallet senderWallet,
                                           Wallet receiverWallet,
                                           User sender,
                                           User receiver) {
        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        receiverWallet.setBalance(receiverWallet.getBalance().add(amount));
        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        Transaction tx = createAndLogTransaction(platformId, type, amount, sender, receiver);
        return tx;
    }

    private Transaction createAndLogTransaction(UUID companyId, TransactionType type, BigDecimal amount, User sender, User receiver) {
        Transaction tx = Transaction.builder()
                .senderId(sender.getId().toString())
                .receiverId(receiver.getId().toString())
                .platformId(companyId.toString())
                .transactionType(type)
                .amount(amount)
                .status(TransactionStatus.SUCCESSFUL)
                .timestamp(Instant.now())
                .build();

        ledgerService.logTransaction(tx);
        transactionRepository.save(tx);
        return tx;
    }


}







