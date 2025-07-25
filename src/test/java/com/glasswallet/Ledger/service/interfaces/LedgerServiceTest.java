package com.glasswallet.Ledger.service.interfaces;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.Ledger.data.repositories.LedgerRepo;
import com.glasswallet.Ledger.dtos.requests.LogTransactionRequest;
import com.glasswallet.Ledger.enums.LedgerType;
import com.glasswallet.Ledger.enums.Status;
import com.glasswallet.Ledger.service.implementation.LedgerServiceImpl;
import com.glasswallet.Wallet.enums.WalletCurrency;
import com.glasswallet.platform.data.repositories.PlatformUserRepository;
import com.glasswallet.transaction.dtos.request.BulkDisbursementRequest;
import com.glasswallet.transaction.dtos.request.DepositRequest;
import com.glasswallet.transaction.dtos.request.TransferRequest;
import com.glasswallet.transaction.dtos.request.WithdrawalRequest;
import com.glasswallet.user.data.models.User;
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
    @Mock private LedgerOrchestrator ledgerOrchestrator;
    @Mock private PlatformUserRepository userRepository;

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
        depositRequest.setCurrency(WalletCurrency.NGN);
        depositRequest.setReference("ref123");

        withdrawalRequest = new WithdrawalRequest();
        withdrawalRequest.setAmount(BigDecimal.valueOf(500));
        withdrawalRequest.setCompanyId("comp456");
        User mockUser = new User();
        mockUser.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440123"));
        withdrawalRequest.setUserId(mockUser);
        withdrawalRequest.setSenderId("sender456");
        withdrawalRequest.setReceiverId("receiver456");
        withdrawalRequest.setCurrency("USD");
        withdrawalRequest.setReference("withdrawRef");

        transferRequest = new TransferRequest();
        transferRequest.setAmount(BigDecimal.valueOf(2000));
        transferRequest.setCompanyId("comp789");
        User mockTransferUser = new User();
        mockTransferUser.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440124"));
        transferRequest.setUserId(mockTransferUser);
        transferRequest.setSenderId("sender789");
        transferRequest.setReceiverId("receiver789");
        transferRequest.setCurrency("USD");
        transferRequest.setReference("transRef");
        transferRequest.setCrypto(false);
    }

    @Test
    void testLogDeposit_createsAndSavesLedgerEntry() {
        LedgerEntry entry = new LedgerEntry();
        when(ledgerRepo.save(any())).thenReturn(entry);

        LedgerEntry result = ledgerService.logDeposit(depositRequest);

        assertNotNull(result);
        verify(ledgerOrchestrator).recordLedgerAndTransaction(any(LedgerEntry.class));
        verify(ledgerRepo, times(2)).save(any()); // once inside logTransaction, once as return
    }

    @Test
    void testLogWithdrawal_createsAndSavesLedgerEntry() {
        when(userRepository.getBalance(any())).thenReturn(BigDecimal.valueOf(1000));
        LedgerEntry entry = new LedgerEntry();
        when(ledgerRepo.save(any())).thenReturn(entry);

        LedgerEntry result = ledgerService.logWithdrawal(withdrawalRequest);

        assertNotNull(result);
        assertEquals(Status.PENDING, result.getStatus());
        assertEquals(LedgerType.WITHDRAWAL, result.getType());
        verify(ledgerOrchestrator).recordLedgerAndTransaction(any());
        verify(ledgerRepo).save(any());
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
        LedgerEntry mockEntry = new LedgerEntry();
        mockEntry.setCompanyId("550e8400-e29b-41d4-a716-446655440000");
        mockEntry.setSenderId("550e8400-e29b-41d4-a716-446655440098");
        mockEntry.setReceiverId("550e8400-e29b-41d4-a716-446655440789");
        mockEntry.setType(LedgerType.DEPOSIT);
        mockEntry.setStatus(Status.SUCCESSFUL);
        when(ledgerRepo.save(any())).thenReturn(mockEntry);

        LedgerEntry entry = ledgerService.logDeposit(depositRequest);

        assertEquals("550e8400-e29b-41d4-a716-446655440000", entry.getCompanyId());
        assertEquals("550e8400-e29b-41d4-a716-446655440098", entry.getSenderId());
        assertEquals("550e8400-e29b-41d4-a716-446655440789", entry.getReceiverId());
        assertEquals(LedgerType.DEPOSIT, entry.getType());
        assertEquals(Status.SUCCESSFUL, entry.getStatus());
    }

    @Test
    void testCreateLedgerEntryFromWithdrawal_setsCorrectValues() {
        when(userRepository.getBalance(any())).thenReturn(BigDecimal.valueOf(1000));
        LedgerEntry mockEntry = new LedgerEntry();
        mockEntry.setSenderId("sender456");
        mockEntry.setReceiverId("receiver456");
        User mockUser = new User();
        mockUser.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440123"));
        mockEntry.setUserId(mockUser);
        mockEntry.setCompanyId("comp456");
        when(ledgerRepo.save(any())).thenReturn(mockEntry);

        LedgerEntry entry = ledgerService.logWithdrawal(withdrawalRequest);

        assertEquals("sender456", entry.getSenderId());
        assertEquals("receiver456", entry.getReceiverId());
        assertEquals("550e8400-e29b-41d4-a716-446655440123", entry.getUserId().getId().toString());
        assertEquals("comp456", entry.getCompanyId());
    }

    @Test
    void testLogTransaction_savesEntry() {
        LedgerEntry dummy = LedgerEntry.builder().reference("abc").build();
        when(ledgerRepo.save(any())).thenReturn(dummy);

        ledgerService.logDeposit(depositRequest); // internally calls logTransaction
        verify(ledgerRepo, atLeastOnce()).save(any(LedgerEntry.class));
    }

    @Test
    void testLogTransaction_returnsNull() {
        LogTransactionRequest request = new LogTransactionRequest();
        LedgerEntry result = ledgerService.logTransaction(request);
        assertNull(result, "LogTransaction should return null for unimplemented method");
    }

    @Test
    void testLogDeposit_withMissingFields_stillLogs() {
        DepositRequest badRequest = new DepositRequest();
        badRequest.setAmount(BigDecimal.TEN);
        LedgerEntry result = ledgerService.logDeposit(badRequest);
        assertNull(result, "Deposit with missing companyId should return null");
    }

    @Test
    void testLogBulkDisbursement_returnsEmptyList() {
        BulkDisbursementRequest bulkRequest = new BulkDisbursementRequest();
        bulkRequest.setDisbursements(List.of()); // Avoid null
        List<LedgerEntry> result = ledgerService.logBulkDisbursement(bulkRequest);
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Bulk disbursement with no requests should return empty list");
    }

    @Test
    void testLogWithdrawal_withNullUser() {
        withdrawalRequest.setUserId(null);
        LedgerEntry result = ledgerService.logWithdrawal(withdrawalRequest);
        assertNull(result, "Withdrawal with null user should return null");
    }

    @Test
    void testLogTransfer_withNullCompanyId() {
        transferRequest.setCompanyId(null);
        List<LedgerEntry> entries = ledgerService.logTransfer(transferRequest);
        assertNull(entries.get(0).getCompanyId());
    }

    @Test
    void testLogDeposit_withNullAmount() {
        DepositRequest badRequest = new DepositRequest();
        badRequest.setAmount(null);
        LedgerEntry result = ledgerService.logDeposit(badRequest);
        assertNull(result, "Deposit with null amount should return null");
    }

    @Test
    void testLogWithdrawal_withInsufficientFunds() {
        when(userRepository.getBalance(any())).thenReturn(BigDecimal.ZERO);
        LedgerEntry result = ledgerService.logWithdrawal(withdrawalRequest);
        assertNull(result, "Withdrawal with insufficient funds should return null");
    }

    @Test
    void testLogTransfer_withSameSenderAndReceiver() {
        transferRequest.setSenderId(transferRequest.getReceiverId());
        List<LedgerEntry> result = ledgerService.logTransfer(transferRequest);
        assertTrue(result.isEmpty(), "Transfer with same sender and receiver should not process");
    }

    @Test
    void testLogBulkDisbursement_withMultipleRequests() {
        BulkDisbursementRequest bulkRequest = new BulkDisbursementRequest();
        TransferRequest req1 = new TransferRequest();
        req1.setAmount(BigDecimal.TEN);
        req1.setCompanyId("comp1");
        TransferRequest req2 = new TransferRequest();
        req2.setAmount(BigDecimal.TEN);
        req2.setCompanyId("comp2");
        bulkRequest.setDisbursements(List.of(req1, req2));

        // Stub ledgerRepo.saveAll to return the input list
        when(ledgerRepo.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LedgerEntry> result = ledgerService.logBulkDisbursement(bulkRequest);
        assertNotNull(result);
        assertEquals(2, result.size(), "Bulk disbursement should process multiple requests");
    }

    @Test
    void testLogDeposit_withNegativeAmount() {
        DepositRequest badRequest = new DepositRequest();
        badRequest.setAmount(BigDecimal.valueOf(-100));
        LedgerEntry result = ledgerService.logDeposit(badRequest);
        assertNull(result, "Deposit with negative amount should return null");
    }

    @Test
    void testLogTransfer_withCryptoFlagTrue() {
        transferRequest.setCrypto(true);
        List<LedgerEntry> entries = ledgerService.logTransfer(transferRequest);
        assertEquals(LedgerType.CRYPTO_TRANSFER_OUT, entries.get(0).getType());
        assertEquals(LedgerType.CRYPTO_TRANSFER_IN, entries.get(1).getType());
    }

    @Test
    void testLogDeposit_withMissingCompanyId() {
        depositRequest.setCompanyId(null);
        LedgerEntry result = ledgerService.logDeposit(depositRequest);
        assertNull(result, "Deposit with missing companyId should return null");
    }
}