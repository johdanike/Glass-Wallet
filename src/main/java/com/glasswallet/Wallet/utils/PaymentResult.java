package com.glasswallet.Wallet.utils;

import com.glasswallet.Wallet.enums.WalletCurrency;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentResult {
        private UUID recipientId;
        private WalletCurrency currency;
        private BigDecimal amount;

        public PaymentResult(UUID id, WalletCurrency currencyType, BigDecimal amount) {
                this.recipientId = id;
                this.currency = currencyType;
                this.amount = amount;
        }
}
