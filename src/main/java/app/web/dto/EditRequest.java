package app.web.dto;

import app.parent.model.ParentRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EditRequest {

    @NotBlank
    @Size(min = 4, max = 16)
    private String password;

    @NotNull
    @Email
    private String email;

    private ParentRole role;


}
