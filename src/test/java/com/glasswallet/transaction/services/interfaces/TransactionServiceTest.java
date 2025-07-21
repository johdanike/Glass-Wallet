package com.glasswallet.transaction.services.interfaces;

import com.glasswallet.Wallet.data.model.Wallet;
import com.glasswallet.Wallet.service.interfaces.WalletService;
import com.glasswallet.transaction.services.implementations.TransactionServiceImpl;
import com.glasswallet.user.data.models.User;
import com.glasswallet.user.services.interfaces.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionServiceTest {

    private final UserService userService;
    private final TransactionService transactionService;
    private final WalletService walletService;
    private Wallet wallet;


    public TransactionServiceTest(UserService userService, TransactionService transactionService, WalletService walletService) {
        this.userService = userService;
        this.transactionService = transactionService;
        this.walletService = walletService;
    }

    @BeforeEach
    public void setup() {
        User user = new User();
        user.setEmail("email");
        user.setPassword("password");
        user.setFirstName("firstName");
        user.setLastName("lastName");
        user.setPlatformUserId("platformUserId");
        user.setAccountNumber("0987654321");



    }



    @Test
    public void testOnboardedUserCanInitiateATransaction() {
        TransactionService transactionService = new TransactionServiceImpl();

        assertNotNull(transactionService);
    }

}