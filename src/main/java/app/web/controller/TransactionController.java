package app.web.controller;

import app.parent.model.Parent;
import app.parent.service.ParentService;
import app.security.UserData;
import app.transaction.model.Transaction;
import app.transaction.service.TransactionService;
import app.wallet.model.Wallet;
import app.wallet.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final ParentService parentService;
    private final WalletService walletService;

    @Autowired
    public TransactionController(TransactionService transactionService, ParentService parentService, WalletService walletService) {
        this.transactionService = transactionService;
        this.parentService = parentService;
        this.walletService = walletService;
    }

    @GetMapping
    public ModelAndView getTransactions(@AuthenticationPrincipal UserData user){
        Parent parent = parentService.getById(user.getUserId());
        Wallet wallet = parent.getWallet();
        if (wallet == null) {
            wallet = walletService.createWallet(parent);
        }
        List<Transaction> transactions = transactionService.getLatestTransactions(wallet.getId());
        ModelAndView modelAndView = new ModelAndView("/transactions");
        modelAndView.addObject("transactions", transactions);
        return modelAndView;

    }

    @GetMapping("/id")
    public ModelAndView getTransactionById(@PathVariable UUID transactionId){
        Transaction transaction = transactionService.getTransactionById(transactionId);
        ModelAndView modelAndView = new ModelAndView("details");
        modelAndView.addObject("transaction", transaction);
        return modelAndView;
    }
}
