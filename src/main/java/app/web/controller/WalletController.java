package app.web.controller;

import app.parent.model.Parent;
import app.parent.service.ParentService;
import app.security.UserData;
import app.transaction.model.Transaction;
import app.wallet.model.Wallet;
import app.wallet.service.WalletService;
import app.web.dto.WalletDepositRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;


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
    @PostMapping
    public String deposit(
            @AuthenticationPrincipal UserData user,
            @Valid @ModelAttribute("walletDepositRequest") WalletDepositRequest walletDepositRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please provide a valid amount.");
            return "redirect:/wallet";
        }

        try {
            UUID parentId = user.getUserId();
            Wallet wallet = walletService.getWalletByParentId(parentId);

            walletService.deposit(wallet.getId(), walletDepositRequest.getAmount(), "Deposit via wallet page");

            redirectAttributes.addFlashAttribute("successMessage", "Money added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/wallet";
    }
//    @PostMapping("/wallet")
//    public String addMoney(@AuthenticationPrincipal UserData user, WalletDepositRequest walletDepositRequest) {
//       Wallet wallet = walletService.getWalletByParentId(user.getUserId());
//       walletService.deposit(wallet.getId(), walletDepositRequest.getAmount(), "Deposit via wallet page");
//       return "redirect:/wallet";
//        // List of transactions must added and updated after deposit
//    }



}
