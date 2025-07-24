package com.glasswallet.Ledger.service.interfaces;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.Ledger.data.repositories.LedgerRepo;
import com.glasswallet.Ledger.enums.LedgerType;
import com.glasswallet.Ledger.enums.Status;
import com.glasswallet.Ledger.service.implementation.LedgerServiceImpl;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.transaction.data.repositories.TransactionRepository;
import com.glasswallet.transaction.dtos.request.BulkDisbursementRequest;
import com.glasswallet.transaction.dtos.request.DepositRequest;
import com.glasswallet.transaction.dtos.request.TransferRequest;
import com.glasswallet.transaction.dtos.request.WithdrawalRequest;
import com.glasswallet.user.data.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock private LedgerRepo ledgerRepo;
    @Mock private TransactionRepository transactionRepository;
    @Mock private LedgerOrchestrator ledgerOrchestrator;
    @Mock private UserRepository userRepository;

    @InjectMocks private LedgerServiceImpl ledgerService;

    private DepositRequest depositRequest;
    private WithdrawalRequest withdrawalRequest;
    private TransferRequest transferRequest;

    @BeforeEach
    void setup() {
        depositRequest = new DepositRequest();
        depositRequest.setAmount(BigDecimal.valueOf(1000));
        depositRequest.setCompanyId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        depositRequest.setSenderId(UUID.fromString("550e8400-e29b-41d4-a716-446655440098"));
        depositRequest.setReceiverId(UUID.fromString("550e8400-e29b-41d4-a716-446655440789"));
        depositRequest.setCurrency(WalletCurrency.valueOf("NGN"));
        depositRequest.setReference("ref123");

        withdrawalRequest = new WithdrawalRequest();
        withdrawalRequest.setAmount(BigDecimal.valueOf(500));
        withdrawalRequest.setCompanyId("comp456");
//        withdrawalRequest.setUserId("user456");
        withdrawalRequest.setSenderId("sender456");
        withdrawalRequest.setReceiverId("receiver456");
        withdrawalRequest.setCurrency("USD");
        withdrawalRequest.setReference("withdrawRef");

        transferRequest = new TransferRequest();
        transferRequest.setAmount(BigDecimal.valueOf(2000));
        transferRequest.setCompanyId("comp789");
//        transferRequest.setUserId("user789");
        transferRequest.setSenderId("sender789");
        transferRequest.setReceiverId("receiver789");
        transferRequest.setCurrency("USD");
        transferRequest.setReference("transRef");
        transferRequest.setCrypto(false);
    }


    @Test
    void testLogDeposit_createsAndSavesLedgerEntry() {
        LedgerEntry entry = mock(LedgerEntry.class);
        when(ledgerRepo.save(any())).thenReturn(entry);

        LedgerEntry result = ledgerService.logDeposit(depositRequest);

        assertNotNull(result);
        verify(ledgerOrchestrator).recordLedgerAndTransaction(any(LedgerEntry.class));
        verify(ledgerRepo, times(2)).save(any()); // once inside logTransaction, once as return
    }


    @Test
    void testLogWithdrawal_createsAndSavesLedgerEntry() {
        LedgerEntry result = ledgerService.logWithdrawal(withdrawalRequest);

        assertNotNull(result);
        assertEquals(Status.PENDING, result.getStatus());
        assertEquals(LedgerType.WITHDRAWAL, result.getType());
        verify(ledgerOrchestrator).recordLedgerAndTransaction(any());
    }


    @Test
    void testLogTransfer_createsTwoEntries() {
        List<LedgerEntry> results = ledgerService.logTransfer(transferRequest);

        assertEquals(2, results.size());
        verify(ledgerOrchestrator, times(2)).recordLedgerAndTransaction(any());
        verify(ledgerRepo).saveAll(any());
    }

    @Test
    void testLogTransfer_cryptoTransfer_hasCorrectLedgerTypes() {
        transferRequest.setCrypto(true);
        List<LedgerEntry> entries = ledgerService.logTransfer(transferRequest);

        assertEquals(LedgerType.CRYPTO_TRANSFER_OUT, entries.get(0).getType());
        assertEquals(LedgerType.CRYPTO_TRANSFER_IN, entries.get(1).getType());
    }

    @Test
    void testLogTransfer_nonCryptoTransfer_hasCorrectLedgerTypes() {
        List<LedgerEntry> entries = ledgerService.logTransfer(transferRequest);

        assertEquals(LedgerType.TRANSFER_OUT, entries.get(0).getType());
        assertEquals(LedgerType.TRANSFER_IN, entries.get(1).getType());
    }

    @Test
    void testLogTransfer_sameReferenceForBothEntries() {
        List<LedgerEntry> entries = ledgerService.logTransfer(transferRequest);
        assertEquals(entries.get(0).getReference(), entries.get(1).getReference());
    }


    @Test
    void testCreateLedgerEntryFromDeposit_setsCorrectValues() {
        LedgerEntry entry = ledgerService.logDeposit(depositRequest);
        assertEquals("550e8400-e29b-41d4-a716-446655440000", entry.getCompanyId());
        assertEquals("550e8400-e29b-41d4-a716-446655440098", entry.getSenderId());
        assertEquals("550e8400-e29b-41d4-a716-446655440789", entry.getReceiverId());
        assertEquals(LedgerType.DEPOSIT, entry.getType());
        assertEquals(Status.SUCCESSFUL, entry.getStatus());
    }


    @Test
    void testCreateLedgerEntryFromWithdrawal_setsCorrectValues() {
        LedgerEntry entry = ledgerService.logWithdrawal(withdrawalRequest);
        assertEquals("550e8400-e29b-41d4-a716-446655440098", entry.getSenderId());
        assertEquals("550e8400-e29b-41d4-a716-446655440789", entry.getReceiverId());
        assertEquals("user456", entry.getUserId());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", entry.getCompanyId());
    }


    @Test
    void testLogTransaction_savesEntry() {
        LedgerEntry dummy = LedgerEntry.builder().reference("abc").build();
        ledgerService.logDeposit(depositRequest); // internally calls logTransaction
        verify(ledgerRepo, atLeastOnce()).save(any(LedgerEntry.class));
    }


//    @Test
//    void testLogTransaction_returnsNull() {
//        LedgerEntry result = ledgerService.logTransaction(new LogTransactionRequest());
//        assertNull(result);
//    }


    @Test
    void testLogBulkDisbursement_returnsEmptyList() {
        List<LedgerEntry> result = ledgerService.logBulkDisbursement(new BulkDisbursementRequest());
        assertTrue(result.isEmpty());
    }


    @Test
    void testLogDeposit_withMissingFields_stillLogs() {
        DepositRequest badRequest = new DepositRequest(); // all null
        badRequest.setAmount(BigDecimal.TEN);
        LedgerEntry result = ledgerService.logDeposit(badRequest);
        assertNotNull(result);
        verify(ledgerRepo, atLeastOnce()).save(any());
    }


    @Test
    void testLogWithdrawal_withNullUserId() {
        withdrawalRequest.setUserId(null);
        LedgerEntry result = ledgerService.logWithdrawal(withdrawalRequest);
        assertNull(result.getUserId());
    }


    @Test
    void testLogTransfer_withNullCompanyId() {
        transferRequest.setCompanyId(null);
        List<LedgerEntry> entries = ledgerService.logTransfer(transferRequest);
        assertNull(entries.get(0).getCompanyId());
    }
}
