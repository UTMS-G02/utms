package edu.iztech.utms.g02.utms_app.api.application.dto;

import edu.iztech.utms.g02.utms_app.dal.user.entity.Staff;
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
// - True → EVALUATION_QUEUE (Pair 3'e devir) // - True → YDYO_APPROVED (Eğer süreç burada bitiyorsa)
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
    private boolean isApproved;

    private String notes; 
    
    private Long reviewerId; 

    //Bu kalmalı mı ? 
    private Staff reviewer; // İnceleyen personelin bilgileri (Eğer token üzerinden alacaksanız bu alana gerek kalmaz)
}