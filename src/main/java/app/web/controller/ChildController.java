package app.web.controller;

import app.child.model.Child;
import app.child.service.ChildService;
import app.parent.model.Parent;
import app.parent.service.ParentService;
import app.security.UserData;
import app.web.dto.ChildRequest;
import app.web.dto.EditChildRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/children")
public class ChildController {

    private final ParentService parentService;
    private final ChildService childService;

    public ChildController(ParentService parentService, ChildService childService) {
        this.parentService = parentService;
        this.childService = childService;
    }

    @GetMapping
    public ModelAndView getChildrenPage(@AuthenticationPrincipal UserData user) {
        Parent parent = parentService.getById(user.getUserId());
        List<Child> children = childService.getChildrenByParentId(parent.getId());
        ModelAndView modelAndView = new ModelAndView("children");
        modelAndView.addObject("parent", parent);
        modelAndView.addObject("children", children);
        modelAndView.addObject("childRequest", new ChildRequest());
        return modelAndView;
    }

    @PostMapping("/registration")
    public String registerChild(@AuthenticationPrincipal UserData user, ChildRequest childRequest) {
        Parent parent = parentService.getById(user.getUserId());
        Child child = childService.registerChild(parent.getId(), childRequest);
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("parent", parent);
        modelAndView.addObject("child", child);
        return "redirect:/children";
    }

    @DeleteMapping("/{childId}")
    public String deleteChild(@AuthenticationPrincipal UserData user, @PathVariable UUID childId) {
        Parent parent = parentService.getById(user.getUserId());
        Child child = childService.getChildById(childId);
        childService.deleteChild(childId);
        return "redirect:/children";
    }

    @GetMapping("/{childId}/child-profile")
    public ModelAndView getChildProfilePage(@PathVariable UUID childId) {
        Child child = childService.getChildById(childId);
        EditChildRequest editChildRequest = new EditChildRequest();
        editChildRequest.setSchool(child.getSchool());
        editChildRequest.setGrade(child.getGrade());
        ModelAndView modelAndView = new ModelAndView("child-profile");
        modelAndView.addObject("child", child);
        modelAndView.addObject("parent", child.getParent()); // Add parent for sidebar
        modelAndView.addObject("editChildRequest", editChildRequest);
        return modelAndView;
    }

    @PutMapping("/{childId}/child-profile")
    public ModelAndView updateChildProfile(@Valid EditChildRequest editChildRequest, BindingResult bindingResult, @PathVariable UUID childId) {

        if (bindingResult.hasErrors()) {
            Child child = childService.getChildById(childId);
            ModelAndView modelAndView = new ModelAndView("child-profile");
            modelAndView.addObject("child", child);
            modelAndView.addObject("parent", child.getParent()); // Add parent for sidebar
            modelAndView.addObject("editChildRequest", editChildRequest);
            return modelAndView;
        }

        childService.updateProfile(childId, editChildRequest);

        return new ModelAndView("redirect:/children");
    }

}
