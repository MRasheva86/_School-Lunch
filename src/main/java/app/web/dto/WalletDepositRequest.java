package app.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WalletDepositRequest {

    @NotNull
    private BigDecimal amount;
}
