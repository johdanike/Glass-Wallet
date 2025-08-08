package com.glasswallet.Wallet.service.implementation;

import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.Wallet.data.repositories.WalletRepository;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.Wallet.service.interfaces.WalletResolver;
import com.glasswallet.Wallet.utils.WalletUtils;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.data.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WalletResolverImpl implements WalletResolver {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public WalletResolverImpl(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    public Optional<Wallet> resolveWallet(String identifier, WalletCurrency currencyType) {
        if (isBlank(identifier)) return Optional.empty();

        Optional<Wallet> wallet = findByAccountNumberOrWalletAddress(identifier, currencyType);
        if (wallet.isPresent()) return wallet;

        wallet = findByUserDetails(identifier, currencyType);
        if (wallet.isPresent()) return wallet;

        if (isPhoneNumber(identifier)) {
            String accountNumber = WalletUtils.generateAccountNumberFromPhoneNumber(identifier);
            return walletRepository.findByAccountNumberAndCurrencyType(accountNumber, currencyType);
        }

        return Optional.empty();
    }

    // Helper Method 1
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    // Helper Method 2
    private Optional<Wallet> findByAccountNumberOrWalletAddress(String identifier, WalletCurrency currencyType) {
        Optional<Wallet> wallet = walletRepository.findByAccountNumberAndCurrencyType(identifier, currencyType);
        if (wallet.isPresent()) return wallet;

        return walletRepository.findByWalletAddressAndCurrencyType(identifier, currencyType);
    }

    // Helper Method 3
    private Optional<Wallet> findByUserDetails(String identifier, WalletCurrency currencyType) {
        Optional<User> user = userRepository.findByEmailOrUsernameOrAccountNumber(identifier, identifier, identifier);
        if (user.isPresent()) {
            return walletRepository.findByUserAndCurrencyType(user.get(), currencyType);
        }
        return Optional.empty();
    }

    // Helper Method 4 (optional)
    private boolean isPhoneNumber(String identifier) {
        return identifier.matches("^\\+?[1-9]\\d{7,14}$");
    }

}
