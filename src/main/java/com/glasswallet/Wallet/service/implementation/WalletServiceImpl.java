package com.glasswallet.Wallet.service.implementation;

import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.Wallet.data.repositories.WalletRepository;
import com.glasswallet.Wallet.dtos.requests.CreateWalletRequest;
import com.glasswallet.Wallet.dtos.response.CreateWalletResponse;
import com.glasswallet.Wallet.dtos.response.WalletBalanceResponse;
import com.glasswallet.Wallet.enums.WalletStatus;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.Wallet.enums.WalletType;
import com.glasswallet.Wallet.exceptions.InsufficientBalanceException;
import com.glasswallet.Wallet.exceptions.InvalidCredentialsException;
import com.glasswallet.Wallet.exceptions.WalletNotFoundException;
import com.glasswallet.Wallet.service.interfaces.WalletResolver;
import com.glasswallet.Wallet.service.interfaces.WalletService;
import com.glasswallet.Wallet.utils.PaymentResult;
import com.glasswallet.Wallet.utils.WalletUtils;
import com.glasswallet.security.JwtUtil;
import com.glasswallet.transaction.services.interfaces.SuiRateService;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.data.repositories.UserRepository;
import com.glasswallet.user.exceptions.UserNotFoundException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class WalletServiceImpl implements WalletService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final JwtUtil jwtUtil;
    private final SuiRateService suiRateService;
    private final WalletResolver walletResolver;
    private final PasswordEncoder passwordEncoder;

    public WalletServiceImpl(UserRepository userRepository,
                             WalletRepository walletRepository,
                             JwtUtil jwtUtil,
                             SuiRateService suiRateService,
                             WalletResolver walletResolver,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.jwtUtil = jwtUtil;
        this.suiRateService = suiRateService;
        this.walletResolver = walletResolver;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public CreateWalletResponse createWalletForUser(String jwtToken, CreateWalletRequest createWalletRequest) {
        UUID userId = UUID.fromString(jwtUtil.extractUserId(jwtToken));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        createWalletIfNotExists(user);

        WalletCurrency currency = WalletUtils.resolveCurrencyFromPhoneNumber(user.getPhoneNumber());
        Wallet wallet = walletRepository.findByUserAndCurrencyType(user, currency)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        return mapToCreateWalletResponse(wallet);
    }

    @Override
    public void createWalletIfNotExists(User user) {
        if (!walletRepository.existsByUserAndCurrencyType(user, WalletCurrency.NGN)) {
            log.info("Creating FIAT wallet for user: {}", user.getEmail());
            createFiatWallet(user);
        }

        if (!walletRepository.existsByUserAndCurrencyType(user, WalletCurrency.SUI)) {
            log.info("Creating CRYPTO wallet for user: {}", user.getEmail());
            createSuiWallet(user);
        }
    }

    @Override
    public CreateWalletResponse createWallet(User user) {
        createWalletIfNotExists(user);
        Wallet fiatWallet = walletRepository.findByUserAndCurrencyType(user, WalletCurrency.NGN)
                .orElseThrow(() -> new WalletNotFoundException("Fiat wallet not found after creation"));
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
    public void withdrawFiat(String recipientIdentifier, BigDecimal amount, String password) throws InvalidCredentialsException {
        Wallet wallet = walletResolver.resolveWallet(recipientIdentifier, WalletCurrency.NGN)
                .orElseThrow(() -> new WalletNotFoundException("Fiat wallet not found"));
        verifyPassword(wallet.getUser(), password);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient fiat balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
    }

    @Override
    public void withdrawSui(String recipientIdentifier, BigDecimal amount, String password) throws InvalidCredentialsException {
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
    public WalletBalanceResponse getUserWalletBalances(String recipientIdentifier, String password) throws InvalidCredentialsException {
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

    @Transactional
    @Override
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
                recipientWallet.getUser().getId(),
                recipientWallet.getCurrencyType(),
                amount
        );
    }

    private Wallet createFiatWallet(User user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setWalletType(WalletType.FIAT);
        wallet.setCurrencyType(WalletCurrency.NGN);
        wallet.setAccountNumber(WalletUtils.generateAccountNumberFromPhoneNumber(user.getPhoneNumber()));
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.ACTIVE);
        return walletRepository.save(wallet);
    }

    private Wallet createSuiWallet(User user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setWalletType(WalletType.CRYPTO);
        wallet.setCurrencyType(WalletCurrency.SUI);
        wallet.setWalletAddress("sui_" + UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        wallet.setTokenSymbol("SUI");
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.ACTIVE);
        return walletRepository.save(wallet);
    }

    private void verifyPassword(User user, String rawPassword) throws InvalidCredentialsException {
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid password");
        }
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
}
