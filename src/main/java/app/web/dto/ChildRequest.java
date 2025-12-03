package app.web.dto;

import app.child.model.ChildGender;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChildRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    private String school;

    @NotBlank
    private int grade;

    private ChildGender gender;

}
