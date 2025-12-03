package app.web.dto;

import app.child.model.Child;
import app.transaction.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDisplayDto {
    private Transaction transaction;
    private Child child;
    private boolean isLunchRelated;

    public static TransactionDisplayDto fromTransaction(Transaction transaction) {
        return new TransactionDisplayDto(transaction, null, false);
    }

    public static TransactionDisplayDto fromTransactionWithChild(Transaction transaction, Child child) {
        return new TransactionDisplayDto(transaction, child, true);
    }
}

