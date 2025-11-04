package app.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EditChildRequest {

    @NotBlank
    private String school;

    @NotBlank
    private int grade;


}
