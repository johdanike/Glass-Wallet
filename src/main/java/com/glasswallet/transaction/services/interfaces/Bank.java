package com.glasswallet.transaction.services.interfaces;

import java.math.BigDecimal;

public interface Bank {
    void transferToAccount(String accountNumber, BigDecimal amount);
}
