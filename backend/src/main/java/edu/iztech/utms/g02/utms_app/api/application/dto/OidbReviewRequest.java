package edu.iztech.utms.g02.utms_app.api.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Data
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OidbReviewRequest {

    @NotNull(message = "Onay durumu (true/false) belirtilmelidir.")
    private boolean approved;

    private String notes; // Red veya onay sebebi (İsteğe bağlı veya reddedildiyse zorunlu yapılabilir)
    

    // TODO: Long kısımları kontrol edilecek.

    private Long reviewerId; // İnceleyen personelin ID'si (Eğer token üzerinden alacaksanız bu alana gerek kalmaz)
}