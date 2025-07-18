package com.glasswallet.user.dtos.responses;

import com.glasswallet.Wallet.data.model.Wallet;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WalletProfileDto {
    private String userId;
    private String email;
    private String phoneNumber;
    private List<Wallet> wallets;

}
