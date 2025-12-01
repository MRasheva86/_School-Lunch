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
import org.junit.jupiter.api.DisplayName;
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

/**
 * Unit tests for {@link WalletService}
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService Tests")
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private app.child.service.ChildService childService;

    @Mock
    private app.lunch.service.LunchService lunchService;

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
    @DisplayName("Should throw exception when payment amount is null")
    void shouldThrowExceptionWhenPaymentAmountIsNull() {
        // When & Then
        DomainException exception = assertThrows(DomainException.class, () -> {
            walletService.payment(walletId, null, description);
        });

        assertEquals("Payment amount must be greater than 0.", exception.getMessage());
        verify(walletRepository, never()).findById(any(UUID.class));
        verify(transactionService, never()).createTransaction(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when payment amount is zero")
    void shouldThrowExceptionWhenPaymentAmountIsZero() {
        // When & Then
        DomainException exception = assertThrows(DomainException.class, () -> {
            walletService.payment(walletId, BigDecimal.ZERO, description);
        });

        assertEquals("Payment amount must be greater than 0.", exception.getMessage());
        verify(walletRepository, never()).findById(any(UUID.class));
        verify(transactionService, never()).createTransaction(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when payment amount is negative")
    void shouldThrowExceptionWhenPaymentAmountIsNegative() {
        // Given
        BigDecimal negativeAmount = new BigDecimal("-50.00");

        // When & Then
        DomainException exception = assertThrows(DomainException.class, () -> {
            walletService.payment(walletId, negativeAmount, description);
        });

        assertEquals("Payment amount must be greater than 0.", exception.getMessage());
        verify(walletRepository, never()).findById(any(UUID.class));
        verify(transactionService, never()).createTransaction(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should retrieve wallet by id when processing payment")
    void shouldRetrieveWalletById() {
        // Given
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        // When
        walletService.payment(walletId, paymentAmount, description);

        // Then
        verify(walletRepository, times(1)).findById(walletId);
    }

    @Test
    @DisplayName("Should handle null balance by treating it as zero")
    void shouldHandleNullBalanceAsZero() {
        // Given
        wallet.setBalance(null);
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(failedTransaction);

        // When
        Transaction result = walletService.payment(walletId, paymentAmount, description);

        // Then
        assertEquals(TransactionStatus.FAILED, result.getStatus());
        verify(transactionService).createTransaction(
                eq(wallet),
                eq(paymentAmount),
                isNull(), // The service passes wallet.getBalance() which is null
                any(),
                any(),
                eq(TransactionStatus.FAILED),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("Should create failed transaction when balance is insufficient")
    void shouldCreateFailedTransactionWhenBalanceInsufficient() {
        // Given
        wallet.setBalance(new BigDecimal("50.00")); // Less than payment amount
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(failedTransaction);

        // When
        Transaction result = walletService.payment(walletId, paymentAmount, description);

        // Then
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
    @DisplayName("Should not update wallet balance when payment fails due to insufficient funds")
    void shouldNotUpdateWalletBalanceWhenPaymentFails() {
        // Given
        BigDecimal originalBalance = new BigDecimal("50.00");
        wallet.setBalance(originalBalance);
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(failedTransaction);

        // When
        walletService.payment(walletId, paymentAmount, description);

        // Then
        verify(walletRepository, never()).save(any(Wallet.class));
        assertEquals(originalBalance, wallet.getBalance());
    }

    @Test
    @DisplayName("Should calculate new balance correctly when payment succeeds")
    void shouldCalculateNewBalanceCorrectly() {
        // Given
        BigDecimal currentBalance = new BigDecimal("200.00");
        wallet.setBalance(currentBalance);
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        // When
        walletService.payment(walletId, paymentAmount, description);

        // Then
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        BigDecimal expectedBalance = currentBalance.subtract(paymentAmount);
        assertEquals(expectedBalance, walletCaptor.getValue().getBalance());
    }

    @Test
    @DisplayName("Should update wallet balance when payment succeeds")
    void shouldUpdateWalletBalanceWhenPaymentSucceeds() {
        // Given
        BigDecimal currentBalance = new BigDecimal("200.00");
        wallet.setBalance(currentBalance);
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        // When
        walletService.payment(walletId, paymentAmount, description);

        // Then
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        assertEquals(currentBalance.subtract(paymentAmount), walletCaptor.getValue().getBalance());
    }

    @Test
    @DisplayName("Should update wallet timestamp when payment succeeds")
    void shouldUpdateWalletTimestampWhenPaymentSucceeds() {
        // Given
        LocalDateTime oldTimestamp = LocalDateTime.now().minusDays(1);
        wallet.setUpdatedOn(oldTimestamp);
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        // When
        walletService.payment(walletId, paymentAmount, description);

        // Then
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        assertNotNull(walletCaptor.getValue().getUpdatedOn());
        assertTrue(walletCaptor.getValue().getUpdatedOn().isAfter(oldTimestamp));
    }

    @Test
    @DisplayName("Should save wallet to repository when payment succeeds")
    void shouldSaveWalletToRepositoryWhenPaymentSucceeds() {
        // Given
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        // When
        walletService.payment(walletId, paymentAmount, description);

        // Then
        verify(walletRepository, times(1)).save(wallet);
    }

    @Test
    @DisplayName("Should create successful transaction with correct parameters when payment succeeds")
    void shouldCreateSuccessfulTransactionWithCorrectParameters() {
        // Given
        BigDecimal currentBalance = new BigDecimal("200.00");
        wallet.setBalance(currentBalance);
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        // When
        walletService.payment(walletId, paymentAmount, description);

        // Then
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
    @DisplayName("Should return successful transaction when payment succeeds")
    void shouldReturnSuccessfulTransactionWhenPaymentSucceeds() {
        // Given
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        // When
        Transaction result = walletService.payment(walletId, paymentAmount, description);

        // Then
        assertNotNull(result);
        assertEquals(TransactionStatus.SUCCESSFUL, result.getStatus());
        assertEquals(TransactionType.PAYMENT, result.getType());
        assertEquals(successfulTransaction, result);
    }

    @Test
    @DisplayName("Should return failed transaction when payment fails")
    void shouldReturnFailedTransactionWhenPaymentFails() {
        // Given
        wallet.setBalance(new BigDecimal("50.00")); // Insufficient funds
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(failedTransaction);

        // When
        Transaction result = walletService.payment(walletId, paymentAmount, description);

        // Then
        assertNotNull(result);
        assertEquals(TransactionStatus.FAILED, result.getStatus());
        assertEquals(TransactionType.PAYMENT, result.getType());
        assertEquals(failedTransaction, result);
    }

    @Test
    @DisplayName("Should check wallet existence before processing payment")
    void shouldCheckWalletExistenceFirst() {
        // Given
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        // When
        walletService.payment(walletId, paymentAmount, description);

        // Then
        var inOrder = inOrder(walletRepository, transactionService);
        inOrder.verify(walletRepository).findById(walletId);
        inOrder.verify(transactionService).createTransaction(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should update balance before creating successful transaction")
    void shouldUpdateBalanceBeforeCreatingSuccessfulTransaction() {
        // Given
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        // When
        walletService.payment(walletId, paymentAmount, description);

        // Then
        var inOrder = inOrder(walletRepository, transactionService);
        inOrder.verify(walletRepository).findById(walletId);
        inOrder.verify(walletRepository).save(wallet);
        inOrder.verify(transactionService).createTransaction(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle payment when balance exactly equals amount")
    void shouldHandlePaymentWhenBalanceExactlyEqualsAmount() {
        // Given
        BigDecimal exactBalance = new BigDecimal("100.00");
        wallet.setBalance(exactBalance);
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        // When
        Transaction result = walletService.payment(walletId, paymentAmount, description);

        // Then
        assertEquals(TransactionStatus.SUCCESSFUL, result.getStatus());
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        assertEquals(new BigDecimal("0.00"), walletCaptor.getValue().getBalance());
    }

    @Test
    @DisplayName("Should handle deposit when balance is null")
    void shouldHandleDepositWhenBalanceIsNull() {
        // Given
        wallet.setBalance(null);
        BigDecimal depositAmount = new BigDecimal("50.00");
        String depositDescription = "Initial deposit";
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        // When
        walletService.deposit(walletId, depositAmount, depositDescription);

        // Then
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        assertEquals(depositAmount, walletCaptor.getValue().getBalance());
    }

    @Test
    @DisplayName("Should treat null balance as zero when depositing")
    void shouldTreatNullBalanceAsZeroWhenDepositing() {
        // Given
        wallet.setBalance(null);
        BigDecimal depositAmount = new BigDecimal("100.00");
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        // When
        walletService.deposit(walletId, depositAmount, "Test deposit");

        // Then
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        assertEquals(depositAmount, walletCaptor.getValue().getBalance());
        
        ArgumentCaptor<BigDecimal> balanceCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(transactionService).createTransaction(
                eq(wallet),
                eq(depositAmount),
                balanceCaptor.capture(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
        assertEquals(depositAmount, balanceCaptor.getValue());
    }

    @Test
    @DisplayName("Should create successful deposit transaction when balance is null")
    void shouldCreateSuccessfulDepositTransactionWhenBalanceIsNull() {
        // Given
        wallet.setBalance(null);
        BigDecimal depositAmount = new BigDecimal("75.00");
        String depositDescription = "Deposit to null balance wallet";
        Transaction depositTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .wallet(wallet)
                .amount(depositAmount)
                .status(TransactionStatus.SUCCESSFUL)
                .type(TransactionType.DEPOSIT)
                .build();
        
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(depositTransaction);

        // When
        Transaction result = walletService.deposit(walletId, depositAmount, depositDescription);

        // Then
        assertNotNull(result);
        assertEquals(TransactionStatus.SUCCESSFUL, result.getStatus());
        assertEquals(TransactionType.DEPOSIT, result.getType());
        
        ArgumentCaptor<TransactionStatus> statusCaptor = ArgumentCaptor.forClass(TransactionStatus.class);
        verify(transactionService).createTransaction(
                eq(wallet),
                eq(depositAmount),
                eq(depositAmount),
                any(),
                eq(TransactionType.DEPOSIT),
                statusCaptor.capture(),
                eq(depositDescription),
                isNull()
        );
        assertEquals(TransactionStatus.SUCCESSFUL, statusCaptor.getValue());
    }

    @Test
    @DisplayName("Should fail payment when balance is null and amount is greater than zero")
    void shouldFailPaymentWhenBalanceIsNullAndAmountIsGreaterThanZero() {
        // Given
        wallet.setBalance(null);
        BigDecimal paymentAmount = new BigDecimal("50.00");
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(failedTransaction);

        // When
        Transaction result = walletService.payment(walletId, paymentAmount, description);

        // Then
        assertEquals(TransactionStatus.FAILED, result.getStatus());
        verify(transactionService).createTransaction(
                eq(wallet),
                eq(paymentAmount),
                isNull(), // The service passes wallet.getBalance() which is null
                any(),
                eq(TransactionType.PAYMENT),
                eq(TransactionStatus.FAILED),
                eq(description),
                eq("Not enough balance in wallet.")
        );
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should not update wallet when payment fails with null balance")
    void shouldNotUpdateWalletWhenPaymentFailsWithNullBalance() {
        // Given
        wallet.setBalance(null);
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(failedTransaction);

        // When
        walletService.payment(walletId, paymentAmount, description);

        // Then
        verify(walletRepository, never()).save(any(Wallet.class));
        assertNull(wallet.getBalance());
    }

    @Test
    @DisplayName("Should handle multiple deposits starting from null balance")
    void shouldHandleMultipleDepositsStartingFromNullBalance() {
        // Given
        wallet.setBalance(null);
        BigDecimal firstDeposit = new BigDecimal("50.00");
        BigDecimal secondDeposit = new BigDecimal("75.00");
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        // When - First deposit
        walletService.deposit(walletId, firstDeposit, "First deposit");
        
        // Verify first deposit saved correctly
        ArgumentCaptor<Wallet> firstWalletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, times(1)).save(firstWalletCaptor.capture());
        assertEquals(firstDeposit, firstWalletCaptor.getValue().getBalance());
        
        // Update wallet balance after first deposit (simulating what the service does)
        wallet.setBalance(firstDeposit);
        
        // When - Second deposit
        walletService.deposit(walletId, secondDeposit, "Second deposit");

        // Then - Verify second deposit
        ArgumentCaptor<Wallet> secondWalletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, times(2)).save(secondWalletCaptor.capture());
        
        List<Wallet> allSavedWallets = secondWalletCaptor.getAllValues();
        assertEquals(firstDeposit.add(secondDeposit), allSavedWallets.get(1).getBalance());
    }

    @Test
    @DisplayName("Should handle deposit then payment with null initial balance")
    void shouldHandleDepositThenPaymentWithNullInitialBalance() {
        // Given
        wallet.setBalance(null);
        BigDecimal depositAmount = new BigDecimal("150.00");
        BigDecimal paymentAmount = new BigDecimal("100.00");
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        // When - Deposit first
        walletService.deposit(walletId, depositAmount, "Initial deposit");
        
        // Verify first deposit
        ArgumentCaptor<Wallet> firstWalletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, times(1)).save(firstWalletCaptor.capture());
        assertEquals(depositAmount, firstWalletCaptor.getValue().getBalance());
        
        // Update wallet balance after deposit (simulating what the service does)
        wallet.setBalance(depositAmount);
        
        // When - Then payment
        walletService.payment(walletId, paymentAmount, "Payment after deposit");

        // Then - Verify payment
        ArgumentCaptor<Wallet> secondWalletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, times(2)).save(secondWalletCaptor.capture());
        
        List<Wallet> allSavedWallets = secondWalletCaptor.getAllValues();
        assertEquals(depositAmount.subtract(paymentAmount), allSavedWallets.get(1).getBalance());
    }

    @Test
    @DisplayName("Should convert null balance to zero in saved wallet after deposit")
    void shouldConvertNullBalanceToZeroInSavedWalletAfterDeposit() {
        // Given
        wallet.setBalance(null);
        BigDecimal depositAmount = new BigDecimal("25.00");
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successfulTransaction);

        // When
        walletService.deposit(walletId, depositAmount, "Test deposit");

        // Then
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        Wallet savedWallet = walletCaptor.getValue();
        assertNotNull(savedWallet.getBalance());
        assertEquals(depositAmount, savedWallet.getBalance());
        assertNotEquals(BigDecimal.ZERO, savedWallet.getBalance());
    }

    @Test
    @DisplayName("Should use null balance in transaction when wallet balance is null for payment")
    void shouldUseNullBalanceInTransactionWhenWalletBalanceIsNullForPayment() {
        // Given
        wallet.setBalance(null);
        BigDecimal paymentAmount = new BigDecimal("30.00");
        when(walletRepository.findById(walletId)).thenReturn(java.util.Optional.of(wallet));
        when(transactionService.createTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(failedTransaction);

        // When
        walletService.payment(walletId, paymentAmount, description);

        // Then
        ArgumentCaptor<BigDecimal> balanceCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(transactionService).createTransaction(
                eq(wallet),
                eq(paymentAmount),
                balanceCaptor.capture(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
        assertNull(balanceCaptor.getValue()); // The service passes wallet.getBalance() which is null
    }
}
