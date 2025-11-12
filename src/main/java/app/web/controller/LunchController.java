package app.web.controller;

import app.child.model.Child;
import app.lunch.model.Lunch;
import app.lunch.service.LunchService;
import app.parent.model.Parent;
import app.parent.service.ParentService;
import app.security.UserData;
import app.web.dto.LunchRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/children/{childId}/lunches")
public class LunchController {

    private final LunchService lunchService;
    private final ParentService parentService;

    public LunchController(LunchService lunchService, ParentService parentService) {
        this.lunchService = lunchService;
        this.parentService = parentService;
    }

    @GetMapping()
    public ModelAndView lunchesPage(@PathVariable("childId")UUID childId, @AuthenticationPrincipal UserData user) {
        ModelAndView modelAndView = new ModelAndView("lunches");
        Parent parent = parentService.getById(user.getUserId());
        if (parent == null) {
            modelAndView.setViewName("login");
        }
        Optional<Child> child = parent.getChildren() == null ? Optional.empty()
                : parent.getChildren().stream().filter(c -> c.getId().equals(childId)).findFirst();
        if (child.isEmpty()) {
             modelAndView.setViewName("children");
        }

        List<Lunch> lunches = lunchService.listByChildId(childId);
        modelAndView.addObject("parent", parent);
        modelAndView.addObject("child", child.get());
        modelAndView.addObject("lunchRequest", new LunchRequest());
        modelAndView.addObject("lunches", lunches);

        return modelAndView;
    }

    @PostMapping
    public String addLunch(@PathVariable("childId")UUID childId, LunchRequest lunchRequest, @AuthenticationPrincipal UserData user) {
        Parent parent = parentService.getById(user.getUserId());
        if (parent == null) {
            return "redirect:/login";
        }
        Optional<Child> child = parent.getChildren() == null ? Optional.empty()
                : parent.getChildren().stream().filter(c -> c.getId().equals(childId)).findFirst();
        if (child.isEmpty()) {
             return "redirect:/children";
        }

        lunchService.addLunch(childId, lunchRequest);
        return "redirect:/children/" + childId + "/lunches";
    }

    // Additional methods for handling order submissions can be added here
    // e.g., @PostMapping to process the order form
    // this must be linked to the Parent and Wallet services to handle order logic, money are paid from Parent's wallet and orders are add to the certain parent's child list


}
