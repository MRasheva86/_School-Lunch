package app.web.controller;

import app.parent.model.Parent;
import app.parent.service.ParentService;
import app.security.UserData;
import app.web.dto.EditRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/home")
public class ParentController {

    private final ParentService parentService;

    @Autowired
    public ParentController(ParentService parentService) {
        this.parentService = parentService;
    }

    @GetMapping("/profile")
    public ModelAndView getEditPage(@AuthenticationPrincipal UserData user,
                                    @ModelAttribute("editRequest") EditRequest editRequest) {
        Parent parent = parentService.getById(user.getUserId());
        ModelAndView modelAndView = new ModelAndView("profile");
        if (editRequest == null || (editRequest.getEmail() == null && editRequest.getPassword() == null)) {
            editRequest = EditRequest.builder()
                    .email(parent.getEmail())
                    .role(parent.getRole())
                    .build();
        }
        modelAndView.addObject("parent", parent);
        modelAndView.addObject("editRequest", editRequest);
        return modelAndView;
    }

    @PostMapping("/profile")
    public String editProfile(@AuthenticationPrincipal UserData user,
                              @Valid @ModelAttribute("editRequest") EditRequest editRequest,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please correct the highlighted fields.");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.editRequest", bindingResult);
            redirectAttributes.addFlashAttribute("editRequest", editRequest);
            return "redirect:/home/profile";
        }

        Parent parent = parentService.getById(user.getUserId());
        parentService.updateProfile(parent.getId(), editRequest);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        return "redirect:/home/profile";
    }




}
