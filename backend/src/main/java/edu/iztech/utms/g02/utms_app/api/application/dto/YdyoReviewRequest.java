package edu.iztech.utms.g02.utms_app.api.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;




/*
// YdyoReviewRequest.java
// YDYO personelinin dil sınavı kararını gönderdiği paket.

// - isApproved: boolean — öğrenci dil şartını sağladı mı?
// - True → EVALUATION_QUEUE (Pair 3'e devir)
// - False → YDYO_REJECTED (süreç biter)
// - 
*/


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