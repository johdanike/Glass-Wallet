package com.glasswallet.transaction.services.implementations;

import com.glasswallet.transaction.services.interfaces.Bank;

import java.math.BigDecimal;

public class PaystackBankAdapter implements Bank {
    @Override
    public void transferToAccount(String accountNumber, BigDecimal amount) {

    }
}
