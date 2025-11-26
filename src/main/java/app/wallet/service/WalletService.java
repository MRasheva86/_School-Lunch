package app.wallet.service;

import app.expetion.DomainException;
import app.parent.model.Parent;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.transaction.model.TransactionType;
import app.transaction.service.TransactionService;
import app.wallet.model.Wallet;
import app.wallet.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionService transactionService;

    @Autowired
    public WalletService(WalletRepository walletRepository, TransactionService transactionService) {
        this.walletRepository = walletRepository;
        this.transactionService = transactionService;
    }

    public Wallet createWallet(Parent parent) {

        Wallet wallet = Wallet.builder()
                .owner(parent)
                .balance(BigDecimal.valueOf(0))
                .currency(Currency.getInstance("EUR"))
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        Wallet savedWallet = walletRepository.save(wallet);

        log.info("Successfully created wallet: {} for parent: {}", savedWallet.getId(), parent.getId());

        return savedWallet;

    }

    @Transactional
    public Transaction deposit(UUID walletId, BigDecimal amount, String description) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Deposit amount must be greater than 0.");
        }

        Wallet wallet = getById(walletId);

        BigDecimal currentBalance = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
        BigDecimal newBalance = currentBalance.add(amount);

        wallet.setBalance(newBalance);
        wallet.setUpdatedOn(LocalDateTime.now());
        walletRepository.save(wallet);

        Transaction transaction = transactionService.createTransaction(
                wallet,
                amount,
                wallet.getBalance(),
                wallet.getCurrency(),
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESSFUL,
                description,
                null
        );

        log.info("Successfully deposited {} to wallet: {}. New balance: {}", amount, walletId, newBalance);

        return transaction;

    }

    @Transactional
    public Transaction payment(UUID walletId, BigDecimal amount, String description) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Payment amount must be greater than 0.");
        }

        Wallet wallet = getById(walletId);

        BigDecimal currentBalance = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();

        if (currentBalance.compareTo(amount) < 0) {

            Transaction transaction = transactionService.createTransaction(
                    wallet,
                    amount,
                    wallet.getBalance(),
                    wallet.getCurrency(),
                    TransactionType.PAYMENT,
                    TransactionStatus.FAILED,
                    description,
                    "Not enough balance in wallet."
            );

            log.warn("Payment failed due to insufficient funds: walletId={}, amount={}. Current balance: {}",
                    walletId, amount, currentBalance);

            return transaction;
        }

        BigDecimal newBalance = currentBalance.subtract(amount);
        wallet.setBalance(newBalance);
        wallet.setUpdatedOn(LocalDateTime.now());
        walletRepository.save(wallet);

        Transaction transaction = transactionService.createTransaction(
                wallet,
                amount,
                wallet.getBalance(),
                wallet.getCurrency(),
                TransactionType.PAYMENT,
                TransactionStatus.SUCCESSFUL,
                description,
                null
        );

        log.info("Successfully processed payment: walletId={}, amount={}. New balance: {}", 
                walletId, amount, newBalance);

        return transaction;

    }

    private Wallet getById(UUID walletId) {
        return walletRepository.findById(walletId).orElseThrow(() -> new DomainException("Wallet by id [%s] was not found.".formatted(walletId)));
    }

    public Wallet getWalletByParentId(UUID parentId) {
        return walletRepository.findByOwnerId(parentId);
    }
    
    public List<Transaction> getTransactionsByWalletId(UUID walletId) {
        return transactionService.getLatestTransactions(walletId);
    }

    @Transactional
    public void deleteWallet(UUID id) {

        transactionService.deleteAllByWalletId(id);
        walletRepository.deleteById(id);

        log.info("Successfully deleted wallet: {}", id);

    }
}
