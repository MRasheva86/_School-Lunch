package app.service;

import app.expetion.DomainException;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.transaction.model.TransactionType;
import app.transaction.service.TransactionService;
import app.wallet.model.Wallet;
import app.wallet.repository.WalletRepository;
import app.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private WalletService walletService;

    private UUID walletId;
    private Wallet wallet;
    private BigDecimal paymentAmount;
    private String description;
    private Transaction successfulTransaction;
    private Transaction failedTransaction;

    @BeforeEach
    void setUp() {

        walletId = UUID.randomUUID();
        paymentAmount = new BigDecimal("100.00");
        description = "Payment for lunch";

        wallet = Wallet.builder()
                .id(walletId)
                .balance(new BigDecimal("200.00"))
                .currency(Currency.getInstance("EUR"))
                .updatedOn(LocalDateTime.now().minusDays(1))
                .build();

        successfulTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .wallet(wallet)
                .amount(paymentAmount)
                .status(TransactionStatus.SUCCESSFUL)
                .type(TransactionType.PAYMENT)
                .build();

        failedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .wallet(wallet)
                .amount(paymentAmount)
                .status(TransactionStatus.FAILED)
                .type(TransactionType.PAYMENT)
                .build();
    }

    @Test
    void shouldThrowExceptionWhenPaymentAmountIsNull() {

        DomainException exception = assertThrows(DomainException.class, () -> {
            walletService.payment(walletId, null, description);
        });

        assertEquals("Payment amount must be greater than 0.", exception.getMessage());
        verify(walletRepository, never()).findById(any(UUID.class));
        verify(transactionService, never()).createTransaction(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenPaymentAmountIsZero() {

        DomainException exception = assertThrows(DomainException.class, () -> {
            walletService.payment(walletId, BigDecimal.ZERO, description);
        });

        assertEquals("Payment amount must be greater than 0.", exception.getMessage());
        verify(walletRepository, never()).findById(any(UUID.class));
        verify(transactionService, never()).createTransaction(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenPaymentAmountIsNegative() {

        BigDecimal negativeAmount = new BigDecimal("-50.00");

        DomainException exception = assertThrows(DomainException.class, () -> {
            walletService.payment(walletId, negativeAmount, description);
        });

        assertEquals("Payment amount must be greater than 0.", exception.getMessage());
        verify(walletRepository, never()).findById(any(UUID.class));
        verify(transactionService, never()).createTransaction(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRetrieveWalletById() {

        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        walletService.payment(walletId, paymentAmount, description);

        verify(walletRepository, times(1)).findById(walletId);
    }

    @Test
    void shouldHandleNullBalanceAsZero() {

        wallet.setBalance(null);
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(failedTransaction);

        Transaction result = walletService.payment(walletId, paymentAmount, description);

        assertEquals(TransactionStatus.FAILED, result.getStatus());
        verify(transactionService).createTransaction(
                eq(wallet),
                eq(paymentAmount),
                isNull(),
                any(),
                any(),
                eq(TransactionStatus.FAILED),
                any(),
                any()
        );
    }

    @Test
    void shouldCreateFailedTransactionWhenBalanceInsufficient() {

        wallet.setBalance(new BigDecimal("50.00"));
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(failedTransaction);

        Transaction result = walletService.payment(walletId, paymentAmount, description);

        ArgumentCaptor<TransactionStatus> statusCaptor = ArgumentCaptor.forClass(TransactionStatus.class);
        ArgumentCaptor<String> failureReasonCaptor = ArgumentCaptor.forClass(String.class);

        verify(transactionService).createTransaction(
                eq(wallet),
                eq(paymentAmount),
                eq(wallet.getBalance()),
                eq(wallet.getCurrency()),
                eq(TransactionType.PAYMENT),
                statusCaptor.capture(),
                eq(description),
                failureReasonCaptor.capture()
        );

        assertEquals(TransactionStatus.FAILED, statusCaptor.getValue());
        assertEquals("Not enough balance in wallet.", failureReasonCaptor.getValue());
        assertEquals(TransactionStatus.FAILED, result.getStatus());
    }

    @Test
    void shouldNotUpdateWalletBalanceWhenPaymentFails() {

        BigDecimal originalBalance = new BigDecimal("50.00");
        wallet.setBalance(originalBalance);
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(failedTransaction);

        walletService.payment(walletId, paymentAmount, description);

        verify(walletRepository, never()).save(any(Wallet.class));
        assertEquals(originalBalance, wallet.getBalance());
    }

    @Test
    void shouldCalculateNewBalanceCorrectly() {

        BigDecimal currentBalance = new BigDecimal("200.00");
        wallet.setBalance(currentBalance);
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        walletService.payment(walletId, paymentAmount, description);

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        BigDecimal expectedBalance = currentBalance.subtract(paymentAmount);
        assertEquals(expectedBalance, walletCaptor.getValue().getBalance());
    }

    @Test
    void shouldUpdateWalletBalanceWhenPaymentSucceeds() {

        BigDecimal currentBalance = new BigDecimal("200.00");
        wallet.setBalance(currentBalance);
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        walletService.payment(walletId, paymentAmount, description);

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        assertEquals(currentBalance.subtract(paymentAmount), walletCaptor.getValue().getBalance());
    }

    @Test
    void shouldUpdateWalletTimestampWhenPaymentSucceeds() {

        LocalDateTime oldTimestamp = LocalDateTime.now().minusDays(1);
        wallet.setUpdatedOn(oldTimestamp);
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        walletService.payment(walletId, paymentAmount, description);

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        assertNotNull(walletCaptor.getValue().getUpdatedOn());
        assertTrue(walletCaptor.getValue().getUpdatedOn().isAfter(oldTimestamp));
    }

    @Test
    void shouldSaveWalletToRepositoryWhenPaymentSucceeds() {

        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        walletService.payment(walletId, paymentAmount, description);

        verify(walletRepository, times(1)).save(wallet);
    }

    @Test
    void shouldCreateSuccessfulTransactionWithCorrectParameters() {

        BigDecimal currentBalance = new BigDecimal("200.00");
        wallet.setBalance(currentBalance);
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        walletService.payment(walletId, paymentAmount, description);

        ArgumentCaptor<BigDecimal> balanceCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<TransactionStatus> statusCaptor = ArgumentCaptor.forClass(TransactionStatus.class);

        verify(transactionService).createTransaction(
                eq(wallet),
                eq(paymentAmount),
                balanceCaptor.capture(),
                eq(wallet.getCurrency()),
                eq(TransactionType.PAYMENT),
                statusCaptor.capture(),
                eq(description),
                isNull()
        );

        BigDecimal expectedBalance = currentBalance.subtract(paymentAmount);
        assertEquals(expectedBalance, balanceCaptor.getValue());
        assertEquals(TransactionStatus.SUCCESSFUL, statusCaptor.getValue());
    }

    @Test
    void shouldReturnSuccessfulTransactionWhenPaymentSucceeds() {

        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        Transaction result = walletService.payment(walletId, paymentAmount, description);

        assertNotNull(result);
        assertEquals(TransactionStatus.SUCCESSFUL, result.getStatus());
        assertEquals(TransactionType.PAYMENT, result.getType());
        assertEquals(successfulTransaction, result);
    }

    @Test
    void shouldReturnFailedTransactionWhenPaymentFails() {

        wallet.setBalance(new BigDecimal("50.00")); // Insufficient funds
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(failedTransaction);

        Transaction result = walletService.payment(walletId, paymentAmount, description);

        assertNotNull(result);
        assertEquals(TransactionStatus.FAILED, result.getStatus());
        assertEquals(TransactionType.PAYMENT, result.getType());
        assertEquals(failedTransaction, result);
    }

    @Test
    void shouldHandlePaymentWhenBalanceExactlyEqualsAmount() {

        BigDecimal exactBalance = new BigDecimal("100.00");
        wallet.setBalance(exactBalance);
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        Transaction result = walletService.payment(walletId, paymentAmount, description);

        assertEquals(TransactionStatus.SUCCESSFUL, result.getStatus());
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        assertEquals(new BigDecimal("0.00"), walletCaptor.getValue().getBalance());
    }

    @Test
    void shouldHandleDepositWhenBalanceIsNull() {

        wallet.setBalance(null);
        BigDecimal depositAmount = new BigDecimal("50.00");
        String depositDescription = "Initial deposit";
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        walletService.deposit(walletId, depositAmount, depositDescription);

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        assertEquals(depositAmount, walletCaptor.getValue().getBalance());
    }

    @Test
    void shouldFailPaymentWhenBalanceIsNullAndAmountIsGreaterThanZero() {

        wallet.setBalance(null);
        BigDecimal paymentAmount = new BigDecimal("50.00");
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(failedTransaction);

        Transaction result = walletService.payment(walletId, paymentAmount, description);

        assertEquals(TransactionStatus.FAILED, result.getStatus());
        verify(transactionService).createTransaction(
                eq(wallet),
                eq(paymentAmount),
                isNull(),
                any(),
                eq(TransactionType.PAYMENT),
                eq(TransactionStatus.FAILED),
                eq(description),
                eq("Not enough balance in wallet.")
        );

        verify(walletRepository, never()).save(any(Wallet.class));
    }

}
