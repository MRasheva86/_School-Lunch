package app.lunch.client.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LunchOrder {
    private String order;

    private LocalDateTime createdOn;

    private String status;
}
