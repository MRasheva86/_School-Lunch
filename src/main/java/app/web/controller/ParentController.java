package app.web.controller;

import app.child.model.Child;
import app.parent.model.Parent;
import app.parent.service.ParentService;
import app.security.UserData;
import app.web.dto.ChildRequest;
import app.web.dto.EditChildRequest;
import app.web.dto.EditRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

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

//    @GetMapping("/profile")
//    public ModelAndView viewProfile(@AuthenticationPrincipal UserData user) {
//        ModelAndView modelAndView = new ModelAndView();
//
//        Parent parent = parentService.getById(user.getUserId());
//        if (parent == null) {
//            modelAndView.setViewName("login");
//        }
//        modelAndView.setViewName("profile");
//        modelAndView.addObject("parent", parent);
//        modelAndView.addObject("editRequest", new EditRequest());
//        return modelAndView;
//    }
//@GetMapping("/profile")
//    public ModelAndView getEditPage(Authentication authentication) {
//
//        String username = authentication.getName();
//        Parent parent = parentService.findByUsername(username);
//        ModelAndView modelAndView = new ModelAndView();
//        modelAndView.setViewName("profile");
//        modelAndView.addObject("parent", parent);
//        return modelAndView;
//    }

    @GetMapping("/{parentId}/profile")
    public ModelAndView getProfilePage(@PathVariable UUID parentId) {
        Parent parent = parentService.getById(parentId);
        EditRequest editRequest = new EditRequest();
        editRequest.setEmail(parent.getEmail());
        editRequest.setPassword(parent.getPassword());
        editRequest.setRole(parent.getRole());

        ModelAndView modelAndView = new ModelAndView("profile");
        modelAndView.addObject("parent", parent);
        modelAndView.addObject("editRequest", editRequest);
        return modelAndView;
    }
    @PutMapping("/{parentId}/profile")
    public ModelAndView updateProfile(@Valid EditRequest editRequest, BindingResult bindingResult, @PathVariable UUID parentId) {

        if (bindingResult.hasErrors()) {
            Parent parent = parentService.getById(parentId);
            ModelAndView modelAndView = new ModelAndView("profile");
            modelAndView.addObject("parent", parent);
            modelAndView.addObject("editRequest", editRequest);
            return modelAndView;
        }

        parentService.updateProfile(parentId, editRequest);

        return new ModelAndView("redirect:/home");
    }
//    @PutMapping("/profile/{id}")
//    public ModelAndView editUser(@Valid EditRequest editRequest, BindingResult bindingResult, @PathVariable UUID parentId) {
//        if (bindingResult.hasErrors()) {
//            Parent parent = parentService.getById(parentId);
//            ModelAndView modelAndView = new ModelAndView("profile");
//            modelAndView.addObject("parent", parent);
//            modelAndView.addObject("editRequest", editRequest);
//            return modelAndView;
//        }
//
//        parentService.updateProfile(parentId, editRequest);
//
//        return new ModelAndView("redirect:/home");
//    }
}
