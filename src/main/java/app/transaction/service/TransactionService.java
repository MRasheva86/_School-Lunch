package app.transaction.service;

import app.expetion.DomainExeption;
import app.wallet.model.Wallet;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.transaction.model.TransactionType;
import app.transaction.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction createTransaction(Wallet wallet, BigDecimal amount, BigDecimal balanceLeft, Currency currency, TransactionType type, TransactionStatus status, String description, String failureReason) {
        log.debug("Creating transaction: walletId={}, type={}, amount={}, status={}", 
                wallet.getId(), type, amount, status);
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

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Successfully created transaction: {} for wallet: {}", savedTransaction.getId(), wallet.getId());
        return savedTransaction;
    }

    public List<Transaction> getLatestTransactions(UUID walletId) {
        log.debug("Getting latest transactions for wallet: {}", walletId);
        return transactionRepository.findTop5ByWallet_IdOrderByCreatedOnDesc(walletId);
    }

    public Transaction getTransactionById(UUID transactionId) {
        log.debug("Getting transaction by id: {}", transactionId);
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new DomainExeption("Transaction with id [%s] not found.".formatted(transactionId)));
    }

    public void deleteAllByWalletId(UUID walletId) {
        log.info("Deleting all transactions for wallet: {}", walletId);
        transactionRepository.deleteAllByWallet_Id(walletId);
        log.info("Successfully deleted all transactions for wallet: {}", walletId);
    }
}
