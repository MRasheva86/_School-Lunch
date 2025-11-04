package app.web.dto;

import app.lunch.model.Meal;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LunchRequest {

    @NotNull
    private Meal meal;

    @NotNull
    private int quantity;

    @NotNull
    private String day;

}
