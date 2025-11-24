package app.web.controller;

import app.parent.model.Parent;
import app.parent.service.ParentService;
import app.wallet.model.Wallet;
import app.wallet.service.WalletService;
import app.web.dto.LoginRequest;
import app.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class IndexController {
    private final ParentService parentService;
    private final WalletService walletService;

    public IndexController(ParentService parentService, WalletService walletService) {
        this.parentService = parentService;
        this.walletService = walletService;
    }

    @GetMapping("/")
    public String getIndexPage() {
        return "index";
    }

    @GetMapping("/login")
    public ModelAndView getLoginPage(@RequestParam(name = "loginAttemptMessage", required = false) String message) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("login");
        modelAndView.addObject("loginRequest", new LoginRequest());
        modelAndView.addObject("loginAttemptMessage", message);
        return modelAndView;
    }

    @GetMapping("/home")
    public ModelAndView getHomePage(Authentication authentication) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("home");

        if (authentication != null && authentication.getName() != null) {
            String username = authentication.getName();
            Parent parent = parentService.findByUsername(username);
            modelAndView.addObject("parent", parent);
            Wallet wallet = walletService.getWalletByParentId(parent.getId());
            if (wallet == null) {
                wallet = walletService.createWallet(parent);
            }
            modelAndView.addObject("wallet", wallet);
        }

        return modelAndView;
    }

    @GetMapping("/register")
    public ModelAndView getRegisterPage() {

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("register");
        modelAndView.addObject("registerRequest", new RegisterRequest());

        return modelAndView;
    }

    @GetMapping("/profile")
    public String redirectProfile() {
        return "redirect:/home/profile";
    }

    @PostMapping("/register")
    public String register(@Valid RegisterRequest registerRequest, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            return "register";
        }
        
        try {
            parentService.register(registerRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (app.expetion.DomainExeption e) {
            // Use the exception message directly (already user-friendly)
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/register";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Handle database constraint violations (e.g., unique constraint on username or email)
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("username") || errorMessage.contains("UK_") || errorMessage.contains("unique"))) {
                redirectAttributes.addFlashAttribute("errorMessage", "This username is already registered. Please chose another one.");
            } else if (errorMessage != null && errorMessage.contains("email")) {
                redirectAttributes.addFlashAttribute("errorMessage", "This email is already registered. Please use another one.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Registration failed. Please try again.");
            }
            return "redirect:/register";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred during registration. Please try again.");
            return "redirect:/register";
        }
    }

}
