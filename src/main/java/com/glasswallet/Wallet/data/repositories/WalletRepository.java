package com.glasswallet.Wallet.data.repositories;

import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.user.data.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    boolean existsByUser(User user);
    Optional<Wallet> findByUserAndCurrencyType(User user, WalletCurrency currencyType);
    List<Wallet> findByUser(User user);
    boolean existsByUserAndCurrencyType(User user, WalletCurrency walletCurrency);
    Optional<Wallet> findByAccountNumberAndCurrencyType(String accountNumber, WalletCurrency currencyType);
    Optional<Wallet> findByWalletAddressAndCurrencyType(String walletAddress, WalletCurrency currencyType);
//    Optional<User> findByEmailOrUsername(String email, String username);


}
