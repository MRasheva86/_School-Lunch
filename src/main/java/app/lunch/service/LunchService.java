package app.lunch.service;

import app.lunch.client.LunchServiceClient;
import app.lunch.model.Lunch;
import app.web.dto.LunchRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LunchService {

    private final LunchServiceClient lunchServiceClient;

    public LunchService(LunchServiceClient lunchServiceClient) {
        this.lunchServiceClient = lunchServiceClient;
    }

    public Lunch addLunch(UUID childId, LunchRequest lunchRequest) {
        return lunchServiceClient.createLunch(childId, lunchRequest);
    }

    public List<Lunch> listByChildId(UUID childId) {
        return lunchServiceClient.getLunches(childId);
    }
}
