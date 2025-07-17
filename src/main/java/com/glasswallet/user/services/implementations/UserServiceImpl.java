package com.glasswallet.user.services.implementations;


import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.Wallet.service.interfaces.WalletService;
import com.glasswallet.security.JwtUtil;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.dtos.responses.LoginResponse;
import com.glasswallet.user.services.interfaces.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final WalletService walletService;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(WalletService walletService, JwtUtil jwtUtil) {
        this.walletService = walletService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginResponse generateLoginResponse(User user, String message) {
        List<Wallet> wallets = walletService.getWallets(user.getId());

        boolean hasFiatWallet = wallets.stream()
                .anyMatch(w -> w.getCurrencyType() == WalletCurrency.NGN);

        boolean hasSuiWallet = wallets.stream()
                .anyMatch(w -> w.getCurrencyType() == WalletCurrency.SUI);

        String fiatAccountNumber = wallets.stream()
                .filter(w -> w.getCurrencyType() == WalletCurrency.NGN)
                .map(Wallet::getAccountNumber)
                .findFirst().orElse(null);

        String suiWalletAddress = wallets.stream()
                .filter(w -> w.getCurrencyType() == WalletCurrency.SUI)
                .map(Wallet::getWalletAddress)
                .findFirst().orElse(null);

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getId(), user.getRole());

        return LoginResponse.builder()
                .userId(String.valueOf(user.getId()))
                .role(user.getRole())
                .message(message)
                .loggedIn(true)
                .hasFiatWallet(hasFiatWallet)
                .hasSuiWallet(hasSuiWallet)
                .fiatWalletAccountNumber(fiatAccountNumber)
                .suiWalletAddress(suiWalletAddress)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }



}
