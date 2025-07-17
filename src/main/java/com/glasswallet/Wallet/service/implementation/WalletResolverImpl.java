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

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public WalletResolverImpl(WalletRepository walletRepository, UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<Wallet> resolveWallet(String identifier, WalletCurrency currencyType) {
        if (identifier == null || identifier.trim().isEmpty()) return Optional.empty();

        // 1. Try account number (FIAT)
        Optional<Wallet> wallet = walletRepository.findByAccountNumberAndCurrencyType(identifier, currencyType);
        if (wallet.isPresent()) return wallet;

        // 2. Try wallet address (CRYPTO)
        wallet = walletRepository.findByWalletAddressAndCurrencyType(identifier, currencyType);
        if (wallet.isPresent()) return wallet;

        // 3. Try user email/username
        Optional<User> user = userRepository.findByEmailOrUsername(identifier, identifier);
        if (user.isPresent()) {
            wallet = walletRepository.findByUserAndCurrencyType(user.get(), currencyType);
            if (wallet.isPresent()) return wallet;
        }

        // 4. Try phone number → account number
        if (identifier.matches("^\\+?[1-9]\\d{7,14}$")) {
            String accountNumber = WalletUtils.generateAccountNumberFromPhoneNumber(identifier);
            return walletRepository.findByAccountNumberAndCurrencyType(accountNumber, currencyType);
        }

        return Optional.empty();
    }
}
