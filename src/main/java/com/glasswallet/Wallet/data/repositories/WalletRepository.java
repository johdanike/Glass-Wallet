package com.glasswallet.Wallet.data.repositories;

import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.user.data.models.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    boolean existsByUser(User user);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user = :user AND w.currencyType = :currency")
    Optional<Wallet> findByUserAndCurrencyTypeWithLock(User user, WalletCurrency currency);
    List<Wallet> findByUser(User user);
    boolean existsByUserAndCurrencyType(User user, WalletCurrency walletCurrency);
    Optional<Wallet> findByAccountNumberAndCurrencyType(String accountNumber, WalletCurrency currencyType);
    Optional<Wallet> findByWalletAddressAndCurrencyType(String walletAddress, WalletCurrency currencyType);
    Optional<Wallet> findByUserAndCurrencyType(User user, WalletCurrency currencyType);


}
