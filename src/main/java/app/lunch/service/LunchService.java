package app.lunch.service;

import app.expetion.ClientErrorException;
import app.lunch.client.LunchServiceClient;
import app.lunch.client.dto.LunchOrder;
import app.lunch.client.dto.LunchOrderRequest;
import app.web.dto.LunchRequest;
import app.wallet.model.Wallet;
import app.wallet.service.WalletService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class LunchService {

    private final LunchServiceClient lunchServiceClient;
    private final WalletService walletService;

    public LunchService(LunchServiceClient lunchServiceClient, WalletService walletService) {
        this.lunchServiceClient = lunchServiceClient;
        this.walletService = walletService;
    }

    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 250)
    )
    public List<LunchOrder> getLunches(UUID childId) {
        log.debug("Getting lunches for child: {}", childId);

        return lunchServiceClient.getLunches(childId);
    }

    @Recover
    public List<LunchOrder> recoverGetLunches(Exception e, UUID childId) {
        log.error("Failed to get lunches after all retry attempts for child: {}", childId, e);

        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "The lunch service is not responding. Please try again later.");
    }

    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 250)
    )
    public List<LunchOrder> getAllLunchesIncludingDeleted(UUID childId) {
        log.debug("Attempting to get all lunches (including deleted) for child: {}", childId);

        return lunchServiceClient.getLunches(childId);
    }
    
    @Recover
    public List<LunchOrder> recoverGetAllLunchesIncludingDeleted(Exception e, UUID childId) {
        log.error("Failed to get all lunches after all retry attempts for child: {}", childId, e);

        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "The lunch service is not responding. Please try again later.");
    }

    @Retryable(
            retryFor = {Exception.class},
            noRetryFor = {ResponseStatusException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 250)
    )
    public void createLunch(UUID parentId, UUID childId, LunchRequest lunchRequest) {

        Wallet wallet = walletService.getWalletByParentId(parentId);

        if (wallet == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found for parent " + parentId);
        }

        LunchOrderRequest payload = LunchOrderRequest.builder()
                .parentId(parentId)
                .childId(childId)
                .walletId(wallet.getId())
                .meal(lunchRequest.getMeal())
                .quantity(lunchRequest.getQuantity())
                .dayOfWeek(lunchRequest.getDayOfWeek())
                .build();

        LunchOrder createdOrder = lunchServiceClient.createLunch(childId, payload);

        BigDecimal walletBalance = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
        
        if (createdOrder.getTotal().compareTo(walletBalance) > 0) {
            try {
                lunchServiceClient.deleteLunch(childId, createdOrder.getId());
                log.debug("Deleted lunch order {} due to insufficient funds", createdOrder.getId());
            } catch (Exception e) {
                log.warn("Failed to delete lunch order {} after payment failure: {}", createdOrder.getId(), e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Failed to pay for lunch order, not enough money in the wallet.");
        }

        walletService.payment(createdOrder.getWalletId(), 
                createdOrder.getTotal(), 
                "Payment for lunch order #" + createdOrder.getId());
    }
    @Recover
    public void recoverCreateLunch(Exception e, UUID parentId, UUID childId, LunchRequest lunchRequest) {

        if (e instanceof ResponseStatusException) {
            throw (ResponseStatusException) e;
        }
        
        log.error("Failed to create lunch after all retry attempts for child: {}", childId, e);

        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "The lunch service is not responding. Please try again later.");
    }

    @Retryable(
            retryFor = {Exception.class},
            noRetryFor = {ClientErrorException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 250)
    )
    public void deleteLunch(UUID childId, UUID lunchId) {
        log.debug("Attempting to delete lunch: childId={}, lunchId={}", childId, lunchId);

        List<LunchOrder> lunches = lunchServiceClient.getLunches(childId);

        LunchOrder lunchToDelete = lunches.stream()
                .filter(lunch -> lunch.getId().equals(lunchId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Lunch order not found: " + lunchId));

        try {
            lunchServiceClient.deleteLunch(childId, lunchId);
        } catch (FeignException e) {
            if (e.status() >= 400 && e.status() < 500) {
                log.debug("Caught 4xx error from lunch-svc: status={}, message={}", 
                        e.status(), e.contentUTF8());
                throw new ClientErrorException(e);
            }
            throw e;
        }

        if (lunchToDelete.getWalletId() != null &&
            lunchToDelete.getTotal() != null && 
            !lunchToDelete.getStatus().equals("COMPLETED")) {
            walletService.deposit(lunchToDelete.getWalletId(), 
                    lunchToDelete.getTotal(), 
                    "Refund for deleted lunch order #" + lunchId);

            log.info("Refund processed for deleted lunch: lunchId={}, amount={}", 
                    lunchId, lunchToDelete.getTotal());
        }
    }
    
    @Recover
    public void recoverDeleteLunch(Exception e, UUID childId, UUID lunchId) {
        if (e instanceof ClientErrorException) {
            throw (ClientErrorException) e;
        }
        
        log.error("Failed to delete lunch after all retry attempts: childId={}, lunchId={}", 
                childId, lunchId, e);

        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "The lunch service is not responding. Please try again later.");
    }

    public List<DayOfWeek> getAvailableDaysForLunch(UUID childId) {
        List<LunchOrder> lunches = getLunches(childId);

        List<String> usedDays = lunches.stream()
                .map(LunchOrder::getDayOfWeek)
                .filter(day -> day != null && !day.isEmpty())
                .map(String::toUpperCase)
                .toList();
        
        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        DayOfWeek todayDayOfWeek = today.getDayOfWeek();
        
        boolean excludeToday = currentTime.isAfter(LocalTime.of(10, 0));
        
        List<DayOfWeek> weekDays = List.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
        );
        
        return weekDays.stream()
                .filter(day -> {
                    String dayName = day.name();
                    if (usedDays.contains(dayName)) {
                        return false;
                    }
                    if (excludeToday && day == todayDayOfWeek) {
                        return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(day -> today.with(TemporalAdjusters.nextOrSame(day))))
                .toList();
    }

    public String getEarliestAvailableDay(UUID childId) {
        List<DayOfWeek> availableDays = getAvailableDaysForLunch(childId);
        if (availableDays.isEmpty()) {
            return null;
        }
        return availableDays.get(0).name();
    }

}

