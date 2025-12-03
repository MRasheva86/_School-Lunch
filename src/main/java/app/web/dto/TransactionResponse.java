package app.web.dto;

import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.transaction.model.TransactionType;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.UUID;

@Value
public class TransactionResponse {
    UUID id;
    UUID walletId;
    BigDecimal amount;
    BigDecimal balanceLeft;
    Currency currency;
    TransactionType type;
    TransactionStatus status;
    String description;
    String failureReason;
    LocalDateTime createdOn;

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getWallet().getId(),
                transaction.getAmount(),
                transaction.getBalanceLeft(),
                transaction.getCurrency(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getFailureReason(),
                transaction.getCreatedOn()
        );
    }
}

