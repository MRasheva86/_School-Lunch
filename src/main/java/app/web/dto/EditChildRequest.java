package app.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EditChildRequest {

    @NotBlank
    private String school;

    @NotNull
    @Min(value = 1)
    @Max(value = 12)
    private Integer grade;

    private MultipartFile image; // Optional image upload
}
