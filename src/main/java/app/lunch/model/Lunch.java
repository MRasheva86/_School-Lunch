package app.lunch.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Lunch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    @Column(nullable = false)
    private UUID childId;

    @Enumerated(EnumType.STRING)
    private app.lunch.model.Meal meal;

    private int quantity;

    private WeekDay weekDay;

    private BigDecimal mealPrice = BigDecimal.valueOf(4.50);

    private BigDecimal totalPrice = mealPrice.multiply(new BigDecimal(quantity));

}
