package app.web.controller;

import app.child.model.Child;
import app.child.service.ChildService;
import app.lunch.client.dto.LunchOrder;
import app.lunch.service.LunchService;
import app.parent.model.Parent;
import app.parent.service.ParentService;
import app.security.UserData;
import app.transaction.model.Transaction;
import app.wallet.model.Wallet;
import app.wallet.service.WalletService;
import app.web.dto.TransactionDisplayDto;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Controller
@RequestMapping("/wallet")
public class WalletController {
    private final ParentService parentService;
    private final WalletService walletService;
    private final ChildService childService;
    private final LunchService lunchService;
    private static final Pattern LUNCH_ORDER_ID_PATTERN = Pattern.compile("lunch order #([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})", Pattern.CASE_INSENSITIVE);

    public WalletController(ParentService parentService, WalletService walletService, 
                           ChildService childService, LunchService lunchService) {
        this.parentService = parentService;
        this.walletService = walletService;
        this.childService = childService;
        this.lunchService = lunchService;
    }

    @GetMapping
    public ModelAndView showWalletPage(@AuthenticationPrincipal UserData user, WalletDepositRequest walletDepositRequest) {
        Parent parent = parentService.getById(user.getUserId());
        Wallet wallet = walletService.getWalletByParentId(parent.getId());
        List<Transaction> transactions = walletService.getTransactionsByWalletId(wallet.getId());
        
        // Enrich transactions with child information for lunch-related transactions
        List<TransactionDisplayDto> transactionDtos = enrichTransactionsWithChildInfo(transactions, parent.getId());
        
        ModelAndView modelAndView = new ModelAndView("wallet");
        modelAndView.addObject("parent", parent);
        modelAndView.addObject("wallet", wallet);
        modelAndView.addObject("walletDepositRequest", walletDepositRequest);
        modelAndView.addObject("transactions", transactionDtos);

        return modelAndView;
    }

    private List<TransactionDisplayDto> enrichTransactionsWithChildInfo(List<Transaction> transactions, UUID parentId) {
        List<TransactionDisplayDto> dtos = new ArrayList<>();
        List<Child> children = childService.getChildrenByParentId(parentId);

        for (Transaction transaction : transactions) {
            String description = transaction.getDescription();
            
            // Check if transaction is lunch-related
            if (description != null && (description.contains("lunch order") || description.contains("Lunch order"))) {
                // Extract lunch order ID from description
                Matcher matcher = LUNCH_ORDER_ID_PATTERN.matcher(description);
                if (matcher.find()) {
                    try {
                        UUID lunchOrderId = UUID.fromString(matcher.group(1));
                        // Find the child associated with this lunch order
                        Child child = findChildByLunchOrderId(children, lunchOrderId);
                        if (child != null) {
                            dtos.add(TransactionDisplayDto.fromTransactionWithChild(transaction, child));
                        } else {
                            dtos.add(TransactionDisplayDto.fromTransaction(transaction));
                        }
                    } catch (IllegalArgumentException e) {
                        // Invalid UUID format, treat as regular transaction
                        dtos.add(TransactionDisplayDto.fromTransaction(transaction));
                    }
                } else {
                    dtos.add(TransactionDisplayDto.fromTransaction(transaction));
                }
            } else {
                dtos.add(TransactionDisplayDto.fromTransaction(transaction));
            }
        }
        return dtos;
    }

    private Child findChildByLunchOrderId(List<Child> children, UUID lunchOrderId) {
        for (Child child : children) {
            try {
                // Get all lunches including deleted ones for transaction lookup
                List<LunchOrder> lunches = lunchService.getAllLunchesIncludingDeleted(child.getId());
                boolean found = lunches.stream()
                        .anyMatch(lunch -> lunch.getId().equals(lunchOrderId));
                if (found) {
                    return child;
                }
            } catch (Exception e) {
                // Continue searching other children
            }
        }
        return null;
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
