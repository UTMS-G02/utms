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
// OidbReviewRequest.java
// ÖİDB personeli inceleme kararını gönderirken kullandığı paket.

// - isApproved: boolean — onaylı mı, reddedildi mi?
// - notes: String — ÖİDB'nin notu
// - Rehberdeki processOidbReview() metodu bunu alır ve durumu değiştirir
// - 
*/


@Data
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OidbReviewRequest {

    @NotNull(message = "Onay durumu (true/false) belirtilmelidir.")
    private boolean isApproved;

    private String notes; // Red veya onay sebebi (İsteğe bağlı veya reddedildiyse zorunlu yapılabilir)

    private boolean requestRevision; // YENİ: Eğer memur düzeltme istiyorsa burası true gelir
    
    // TODO: Long kısımları kontrol edilecek.

    private Long reviewerId; // İnceleyen personelin ID'si (Eğer token üzerinden alacaksanız bu alana gerek kalmaz)

    //Bu kalmalı mı ? 
    private Staff reviewer; // İnceleyen personelin bilgileri (Eğer token üzerinden alacaksanız bu alana gerek kalmaz)
}