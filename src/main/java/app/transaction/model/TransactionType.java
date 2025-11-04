package app.transaction.model;

import jakarta.persistence.Enumerated;

public enum TransactionType {
    DEPOSIT,
    PAYMENT,
    REFUND
}
