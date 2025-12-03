package app.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LunchRequest {

    @NotBlank
    private String dayOfWeek = "MONDAY";

    @NotBlank
    private String meal = "FRIED_CHICKEN_WITH_YOGURT_SOUS";

    @Min(1)
    @NotNull
    private Integer quantity = 1;
}

