package app.web.controller;

import app.child.service.ChildService;
import app.expetion.ClientErrorException;
import app.lunch.client.dto.LunchOrder;
import app.lunch.client.dto.MealOption;
import app.lunch.service.LunchService;
import app.security.UserData;
import app.web.dto.LunchRequest;
import app.web.util.ErrorMessageExtractor;
import feign.FeignException;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/children")
public class LunchController {

    private final ChildService childService;
    private final LunchService lunchService;
    private final ErrorMessageExtractor errorMessageExtractor;

    public LunchController(ChildService childService, LunchService lunchService, 
                           ErrorMessageExtractor errorMessageExtractor) {
        this.childService = childService;
        this.lunchService = lunchService;
        this.errorMessageExtractor = errorMessageExtractor;
    }


    @GetMapping("/{childId}/lunches")
    public ModelAndView getLunches(@AuthenticationPrincipal UserData userData,
                                   @PathVariable UUID childId, @ModelAttribute("lunchRequest") LunchRequest lunchRequest) {

        childService.checkChildParent(userData.getUserId(), childId);
        
        List<LunchOrder> lunches = Collections.emptyList();

        String errorMessage = null;

        try {
            lunches = lunchService.getLunches(childId);
        } catch (FeignException e) {
            errorMessage = errorMessageExtractor.extractErrorMessage(e);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE) {
                errorMessage = "The lunch service is not responding. Please try again later.";
            } else {
                errorMessage = e.getReason();
            }
        } catch (Exception e) {
            errorMessage = "An error occurred while loading lunches. Please try again later.";
        }

        List<DayOfWeek> availableDays = lunchService.getAvailableDaysForLunch(childId);
        String earliestDay = lunchService.getEarliestAvailableDay(childId);

        ModelAndView modelAndView = new ModelAndView("lunches");
        modelAndView.addObject("child", childService.getChildById(childId));
        modelAndView.addObject("parent", childService.getChildById(childId).getParent());
        modelAndView.addObject("lunches", lunches);
        modelAndView.addObject("mealOptions", MealOption.values());
        modelAndView.addObject("dayOptions", availableDays);
        
        if (errorMessage != null) {
            modelAndView.addObject("errorMessage", errorMessage);
        }
        
        if (lunches.isEmpty() && errorMessage == null) {
            modelAndView.addObject("infoMessage", "No lunches ordered.");
        }
        if (availableDays.isEmpty() && errorMessage == null) {
            modelAndView.addObject("infoMessage", "Great! All days of the week are covered for lunch.");
        }
        
        if (lunchRequest == null) {
            lunchRequest = new LunchRequest();
        }
        if (earliestDay != null) {
            String currentDayOfWeek = lunchRequest.getDayOfWeek();
            if (currentDayOfWeek == null || 
                availableDays.stream().noneMatch(day -> day.name().equals(currentDayOfWeek)) ||
                !earliestDay.equals(currentDayOfWeek)) {
                lunchRequest.setDayOfWeek(earliestDay);
            }
        }

        modelAndView.addObject("lunchRequest", lunchRequest);
        return modelAndView;

    }

    @PostMapping("/{childId}/lunches")
    public ModelAndView createLunch(@AuthenticationPrincipal UserData userData, @PathVariable UUID childId,
                                    @Valid @ModelAttribute("lunchRequest") LunchRequest lunchRequest,
                                    BindingResult bindingResult, RedirectAttributes redirectAttributes) {

        childService.checkChildParent(userData.getUserId(), childId);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix the highlighted errors.");
            redirectAttributes.addFlashAttribute("validLunchRequest", bindingResult);
            redirectAttributes.addFlashAttribute("lunchRequest", lunchRequest);
            return new ModelAndView("redirect:/children/" + childId + "/lunches");
        }

        try {
            lunchService.createLunch(userData.getUserId(), childId, lunchRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Lunch added successfully!");
        } catch (FeignException e) {
            redirectAttributes.addFlashAttribute("errorMessage", errorMessageExtractor.extractErrorMessage(e));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            if (e.getStatusCode() == org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                        "The lunch service is not responding. Please try again later.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", e.getReason());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "An error occurred while creating lunch. Please try again later.");
        }

        return new ModelAndView("redirect:/children/" + childId + "/lunches");
    }

    @DeleteMapping("/{childId}/lunches/{lunchId}")
    public ModelAndView deleteLunch(@AuthenticationPrincipal UserData userData, @PathVariable UUID childId,
                                    @PathVariable UUID lunchId, RedirectAttributes redirectAttributes) {

        childService.checkChildParent(userData.getUserId(), childId);

        try {
            lunchService.deleteLunch(childId, lunchId);
            redirectAttributes.addFlashAttribute("successMessage", "Lunch removed.");
        } catch (ClientErrorException e) {
            String errorMessage = errorMessageExtractor.extractErrorMessage(e.getFeignException());
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
        } catch (FeignException e) {
            redirectAttributes.addFlashAttribute("errorMessage", errorMessageExtractor.extractErrorMessage(e));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            if (e.getStatusCode() == org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                        "The lunch service is not responding. Please try again later.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", e.getReason());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "An error occurred while deleting lunch. Please try again later.");
        }

        return new ModelAndView("redirect:/children/" + childId + "/lunches");
    }
}

