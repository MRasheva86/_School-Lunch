package app.web.controller;

import app.parent.model.Parent;
import app.parent.service.ParentService;
import app.security.UserData;
import app.web.dto.EditRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;


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
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView getUsers() {

        List<Parent> users = parentService.getAllParents();

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("users");
        modelAndView.addObject("users", users);

        return modelAndView;
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(@PathVariable UUID userId, RedirectAttributes redirectAttributes) {
        parentService.deleteParent(userId);
        redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully!");

        return "redirect:/home/users";
    }

    @PatchMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateUserRole(@PathVariable UUID userId,
                                 @RequestParam("role") String newRole,
                                 RedirectAttributes redirectAttributes) {
        parentService.updateUserRole(userId, newRole);
        redirectAttributes.addFlashAttribute("successMessage", "User role updated successfully!");
        return "redirect:/home/users";
    }



}
