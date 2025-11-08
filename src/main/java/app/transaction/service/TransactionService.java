package app.transaction.service;

import app.expetion.DomainExeption;
import app.wallet.model.Wallet;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.transaction.model.TransactionType;
import app.transaction.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction createTransaction(Wallet wallet, BigDecimal amount, BigDecimal balanceLeft, Currency currency, TransactionType type, TransactionStatus status, String description, String failureReason) {
        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .currency(currency)
                .balanceLeft(balanceLeft)
                .type(type)
                .status(status)
                .description(description)
                .failureReason(failureReason)
                .createdOn(LocalDateTime.now())
                .build();

        return transactionRepository.save(transaction);
    }

    public List<Transaction> getLatestTransactions(UUID walletId) {
        return transactionRepository.findTop5ByWallet_IdOrderByCreatedOnDesc(walletId);
    }

    public Transaction getTransactionById(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new DomainExeption("Transaction with id [%s] not found.".formatted(transactionId)));
    }

    public void deleteAllByWalletId(UUID walletId) {

        transactionRepository.deleteAllByWallet_Id(walletId);
    }
}
