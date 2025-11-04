package app.web.controller;

import app.parent.model.Parent;
import app.parent.service.ParentService;
import app.web.dto.EditRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;


@Controller
@RequestMapping("/home")
public class UserController {

    private final ParentService parentService;

    @Autowired
    public UserController(ParentService parentService) {
        this.parentService = parentService;
    }
    @GetMapping("/edit")
    public ModelAndView getEditPage(Authentication authentication) {

        String username = authentication.getName();
        Parent parent = parentService.findByUsername(username);
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("profile");
        modelAndView.addObject("parent", parent);
        return modelAndView;
    }

    @PatchMapping("/edit")
    public String editUser(@Valid EditRequest editRequest, BindingResult bindingResult, Authentication authentication) {

        if (bindingResult.hasErrors()) {
            return "home";
        }
        String username = authentication.getName();
        parentService.editParent(username, editRequest);
        return "redirect:/home";
    }
}
