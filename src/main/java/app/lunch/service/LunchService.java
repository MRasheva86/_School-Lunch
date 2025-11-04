package app.lunch.service;

import app.lunch.model.Lunch;
import app.lunch.model.WeekDay;
import app.lunch.repository.LunchRepository;
import app.web.dto.LunchRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class LunchService {

    private final LunchRepository lunchRepository;

    public LunchService(LunchRepository lunchRepository) {
        this.lunchRepository = lunchRepository;
    }

    public Lunch addLunch(UUID childId, LunchRequest lunchRequest) {

       Lunch lunch = Lunch.builder()
                .childId(childId)
                .meal(lunchRequest.getMeal())
                .quantity(lunchRequest.getQuantity())
                .weekDay(WeekDay.valueOf(lunchRequest.getDay()))
                .mealPrice(BigDecimal.valueOf(4.50).multiply(BigDecimal.valueOf(lunchRequest.getQuantity())))
                .build();

        return lunchRepository.save(lunch);
    }
    public List<Lunch> listByChildId(UUID childId) {
        return lunchRepository.findByChildIdOrderByWeekDayDesc(childId);
    }
}
