package app.web.controller;

import app.transaction.model.Transaction;
import app.wallet.service.WalletService;
import app.web.dto.TransactionResponse;
import app.web.dto.WalletOperationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletApiController {

    private final WalletService walletService;

    public WalletApiController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/{walletId}/credit")
    public ResponseEntity<TransactionResponse> credit(@PathVariable UUID walletId,
                                                      @Valid @RequestBody WalletOperationRequest request) {

        Transaction transaction = walletService.deposit(walletId, request.getAmount(),
                request.getDescription() != null ? request.getDescription() : "External credit");

        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(transaction));
    }

    @PostMapping("/{walletId}/debit")
    public ResponseEntity<TransactionResponse> debit(@PathVariable UUID walletId,
                                                     @Valid @RequestBody WalletOperationRequest request) {

        Transaction transaction = walletService.payment(walletId, request.getAmount(),
                request.getDescription() != null ? request.getDescription() : "External debit");

        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(transaction));
    }
}

