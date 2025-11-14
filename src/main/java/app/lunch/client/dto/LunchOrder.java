package app.lunch.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class LunchOrder {
    private UUID id;
    private UUID parentId;
    private UUID childId;
    private String meal;
    private Integer quantity;
    private String dayOfWeek;
    private BigDecimal unitPrice;
    private BigDecimal total;
    private String status;
    private Instant createdOn;
}
