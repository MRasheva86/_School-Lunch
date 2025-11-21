package app.wallet.service;

import app.expetion.DomainExeption;
import app.parent.model.Parent;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.transaction.model.TransactionType;
import app.transaction.service.TransactionService;
import app.wallet.model.Wallet;
import app.wallet.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

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

        return walletRepository.save(wallet);
    }

    @Transactional
    public Transaction deposit(UUID walletId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainExeption("Deposit amount must be greater than 0.");
        }

        Wallet wallet = getById(walletId);

        BigDecimal currentBalance = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
        BigDecimal newBalance = currentBalance.add(amount);

        wallet.setBalance(newBalance);
        wallet.setUpdatedOn(LocalDateTime.now());
        walletRepository.save(wallet);

        return transactionService.createTransaction(
                wallet,
                amount,
                wallet.getBalance(),
                wallet.getCurrency(),
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESSFUL,
                description,
                null
        );
    }

    @Transactional
    public Transaction payment(UUID walletId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainExeption("Payment amount must be greater than 0.");
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
            return transaction;
        }

        BigDecimal newBalance = currentBalance.subtract(amount);
        wallet.setBalance(newBalance);
        wallet.setUpdatedOn(LocalDateTime.now());
        walletRepository.save(wallet);

        return transactionService.createTransaction(
                wallet,
                amount,
                wallet.getBalance(),
                wallet.getCurrency(),
                TransactionType.PAYMENT,
                TransactionStatus.SUCCESSFUL,
                description,
                null
        );
    }

    private Wallet getById(UUID walletId) {
        return walletRepository.findById(walletId).orElseThrow(() -> new DomainExeption("Wallet by id [%s] was not found.".formatted(walletId)));
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
    }
}
