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

        Wallet wallet = getById(walletId);

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        return transactionService.createTransaction(
                wallet.getOwner(),
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

        Wallet wallet = getById(walletId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            return transactionService.createTransaction(
                    wallet.getOwner(),
                    amount,
                    wallet.getBalance(),
                    wallet.getCurrency(),
                    TransactionType.PAYMENT,
                    TransactionStatus.FAILED,
                    description,
                    "Not enough balance in wallet."
            );
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        return transactionService.createTransaction(
                wallet.getOwner(),
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
        Wallet wallet = getById(walletId);
        return transactionService.getAllTransactions(wallet.getOwner().getId());
    }
}
