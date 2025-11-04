package app.web.controller;

import app.parent.model.Parent;
import app.parent.service.ParentService;
import app.security.UserData;
import app.transaction.model.Transaction;
import app.wallet.model.Wallet;
import app.wallet.service.WalletService;
import app.web.dto.WalletDepositRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;


@Controller
@RequestMapping("/wallet")
public class WalletController {
    private final ParentService parentService;
    private final WalletService walletService;

    public WalletController(ParentService parentService, WalletService walletService) {
        this.parentService = parentService;
        this.walletService = walletService;
    }

    @GetMapping
    public ModelAndView showWalletPage(@AuthenticationPrincipal UserData user, WalletDepositRequest walletDepositRequest) {
        Parent parent = parentService.getById(user.getUserId());
        Wallet wallet = walletService.getWalletByParentId(parent.getId());
        List<Transaction> transactions = walletService.getTransactionsByWalletId(wallet.getId());
        ModelAndView modelAndView = new ModelAndView("wallet");

        modelAndView.addObject("parent", parent);
        modelAndView.addObject("wallet", wallet);
        modelAndView.addObject("walletDepositRequest", walletDepositRequest);
        modelAndView.addObject("transactions", transactions);

        return modelAndView;
    }
    @PostMapping("/wallet")
    public String addMoney(@AuthenticationPrincipal UserData user, WalletDepositRequest walletDepositRequest) {
       Wallet wallet = walletService.getWalletByParentId(user.getUserId());
       walletService.deposit(wallet.getId(), walletDepositRequest.getAmount(), "Deposit via wallet page");
       return "redirect:/wallet";
        // List of transactions must added and updated after deposit
    }



}
