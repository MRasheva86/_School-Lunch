package app.wallet.service;

import app.child.model.Child;
import app.child.service.ChildService;
import app.expetion.DomainException;
import app.lunch.client.dto.LunchOrder;
import app.lunch.service.LunchService;
import app.parent.model.Parent;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.transaction.model.TransactionType;
import app.transaction.service.TransactionService;
import app.web.dto.TransactionDisplayDto;
import app.wallet.model.Wallet;
import app.wallet.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionService transactionService;
    private final ChildService childService;
    private final LunchService lunchService;
    private static final Pattern LUNCH_ORDER_ID_PATTERN = Pattern.compile(
            "lunch order #([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})", 
            Pattern.CASE_INSENSITIVE);

    @Autowired
    public WalletService(WalletRepository walletRepository, TransactionService transactionService,
                        @Lazy ChildService childService, @Lazy LunchService lunchService) {
        this.walletRepository = walletRepository;
        this.transactionService = transactionService;
        this.childService = childService;
        this.lunchService = lunchService;
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

    public Wallet getOrCreateWallet(Parent parent) {
        Wallet wallet = getWalletByParentId(parent.getId());
        if (wallet == null) {
            wallet = createWallet(parent);
        }
        return wallet;
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

    public List<TransactionDisplayDto> enrichTransactionsWithChildInfo(List<Transaction> transactions, UUID parentId) {
        log.debug("Enriching {} transactions with child info for parent: {}", transactions.size(), parentId);
        List<TransactionDisplayDto> dtos = new ArrayList<>();
        List<Child> children = childService.getChildrenByParentId(parentId);

        for (Transaction transaction : transactions) {
            String description = transaction.getDescription();
            
            if (description != null && (description.contains("lunch order") || description.contains("Lunch order"))) {
                Matcher matcher = LUNCH_ORDER_ID_PATTERN.matcher(description);
                if (matcher.find()) {
                    try {
                        UUID lunchOrderId = UUID.fromString(matcher.group(1));
                        Child child = findChildByLunchOrderId(children, lunchOrderId);
                        if (child != null) {
                            dtos.add(TransactionDisplayDto.fromTransactionWithChild(transaction, child));
                        } else {
                            dtos.add(TransactionDisplayDto.fromTransaction(transaction));
                        }
                    } catch (IllegalArgumentException e) {
                        log.debug("Invalid UUID format in transaction description: {}", description);
                        dtos.add(TransactionDisplayDto.fromTransaction(transaction));
                    }
                } else {
                    dtos.add(TransactionDisplayDto.fromTransaction(transaction));
                }
            } else {
                dtos.add(TransactionDisplayDto.fromTransaction(transaction));
            }
        }
        log.debug("Enriched {} transactions with child info", dtos.size());
        return dtos;
    }
    
    private Child findChildByLunchOrderId(List<Child> children, UUID lunchOrderId) {
        for (Child child : children) {
            try {
                List<LunchOrder> lunches = lunchService.getAllLunchesIncludingDeleted(child.getId());
                boolean found = lunches.stream()
                        .anyMatch(lunch -> lunch.getId().equals(lunchOrderId));
                if (found) {
                    return child;
                }
            } catch (Exception e) {
                log.debug("Error while searching for lunch order {} in child {}: {}", 
                        lunchOrderId, child.getId(), e.getMessage());
                // Continue searching other children
            }
        }
        return null;
    }
}
