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
public class YdyoReviewRequest {

    @NotNull(message = "Onay durumu (true/false) belirtilmelidir.")
    private boolean approved;

    private String notes; 
    
    private Long reviewerId; 
}