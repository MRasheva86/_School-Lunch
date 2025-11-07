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

    @PostMapping("/register")
    public String register(@Valid RegisterRequest registerRequest, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            return "register";
        }
        
        try {
            parentService.register(registerRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/register";
        }
    }

}
