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
 YdyoExamResultRequest.java
 YDYO personelinin dil sınavı sonucunu gönderdiği paket.

 - examScore: öğrencinin sınavdan aldığı not (örneğin 85.5)
 - notes: inceleyen personelin eklemek istediği notlar
 - reviewerId: inceleyen personelin ID'si (opsiyonel, token üzerinden de alınabilir)
 - reviewer: inceleyen personelin bilgileri (Eğer token üzerinden alacaksanız bu alana gerek kalmaz)
*/


@Data
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class YdyoExamResultRequest {
    private Double examScore; // Öğrencinin aldığı not
    private Boolean passed;   // YENİ: Personelin manuel "Geçti/Kaldı" kararı
    private String notes;
    private Integer reviewerId;
    private Staff reviewer;
    
}
