package app.lunch.client.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LunchOrderRequest {

    private UUID parentId;

    private UUID childId;

    private String meal;

    private int quantity;

    private String dayOfWeek;
}
