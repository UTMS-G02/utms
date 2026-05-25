package edu.iztech.utms.g02.utms_app.api.application.controller; 

import edu.iztech.utms.g02.utms_app.api.application.dto.*;
import edu.iztech.utms.g02.utms_app.bl.application.ApplicationService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus; //eklendi
//import org.springframework.http.MediaType; //eklendi
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;      //yetkilendirme
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;                
/*
// HTTP üzerinden dosya (PDF, JPG, PNG vb.) gönderilirken bu dosyalar bayt (byte) akışları halinde gelir. 
// Spring Boot, bu karmaşık akışı senin için alıp kullanımı çok kolay olan MultipartFile nesnesine dönüştürür. 
// file.getOriginalFilename(), file.getSize() gibi metotlarla dosyayı rahatça işlemeni sağlayan standart Spring arayüzüdür.
 */


import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/applications") // Tüm endpointlerin ortak başlangıcı
public class ApplicationController {

    private final ApplicationService applicationService;

    /*
    /    POST /api/applications
    */

    @PreAuthorize("hasRole('STUDENT')") // Sadece STUDENT rolü girebilir
    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(@RequestBody ApplicationCreateRequest req) {
        // Not: Gerçek projede userId'yi SecurityContext'ten (token'dan) alırsın.
        // ID artık token üzerinden Service katmanında alındığı için currentUserId parametresini kaldırdık.
        //Integer currentUserId = 1; // Şimdilik temsili
        ApplicationResponse response = applicationService.create(req);
        //return ResponseEntity.ok(response);
        
        //EKLENDİ
        // REST standartlarına göre yeni bir kaynak oluşturulduğunda HTTP 201 (Created) dönülür.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    /*
    /   PATCH /api/applications/{id}/submit
    */

    @PreAuthorize("hasRole('STUDENT')") 
    @PatchMapping("/{id}/submit")
    public ResponseEntity<ApplicationResponse> submitApplication(@PathVariable Integer id) {
        // Öğrencinin sadece kendi başvurusunu gönderebilmesi kontrolü Service katmanında yapılır
        // Ayrıca String dönmek yerine güncel ApplicationResponse'u dönüyoruz ki front-end sayfayı yenilemeden veriyi güncelleyebilsin.
        ApplicationResponse response = applicationService.submit(id);
        return ResponseEntity.ok(response);
        //applicationService.submit(id);
        //return ResponseEntity.ok("Başvuru başarıyla gönderildi.");
    }


    /*
    /   GET /api/applications
    */

    @PreAuthorize("hasAnyRole('STUDENT', 'OIDB', 'YDYO')") // 3 rolden biri yeterli
    @GetMapping
    public ResponseEntity<List<ApplicationResponse>> getAllApplications() {
        // Service katmanında, istek atan kişinin rolüne göre farklı listeler dönecek bir mantık kurulmalı
        List<ApplicationResponse> list = applicationService.getAllApplications();
        return ResponseEntity.ok(list);
    }


    @PreAuthorize("hasAnyRole('STUDENT', 'OIDB', 'YDYO', 'FACULTY', 'DEAN')") 
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationById(@PathVariable Integer id) {
        ApplicationResponse response = applicationService.getApplicationById(id);
        return ResponseEntity.ok(response);
    }



    @PreAuthorize("hasRole('OIDB')")
    @PatchMapping("/{id}/oidb-review")
    public ResponseEntity<ApplicationResponse> reviewByOidb(@PathVariable Integer id, @RequestBody OidbReviewRequest req) {
        ApplicationResponse response = applicationService.processOidbReview(id, req);
        return ResponseEntity.ok(response);
    }



    @PreAuthorize("hasRole('YDYO')")
    @PatchMapping("/{id}/ydyo-review")
    public ResponseEntity<ApplicationResponse> reviewByYdyo(@PathVariable Integer id, @RequestBody YdyoReviewRequest req) {
        ApplicationResponse response = applicationService.processYdyoReview(id, req);
        return ResponseEntity.ok(response);
    }


    @PreAuthorize("hasRole('STUDENT')")
    // consumes = "multipart/form-data" yazmak doğrudur ancak Spring'in kendi sabitini (MediaType) kullanmak daha profesyoneldir.
    @PostMapping(value = "/{id}/documents", consumes = "multipart/form-data")  //MediaType.MULTIPART_FORM_DATA_VALUE da kullanlabilirmiş ?!
    public ResponseEntity<String> uploadDocument(
            @PathVariable Integer id, 
            @RequestParam("file") MultipartFile file) {
        
        applicationService.uploadDocument(id, file); //!!!! bu method incelenecek ve endpoint incelenip düzeltilecek 
        return ResponseEntity.ok("Belge başarıyla yüklendi.");
    }

}

/*
// consumes:
// Genelde REST API'ler JSON formatında konuşur (application/json). 
// Ancak dosya yükleme (upload) işlemlerinde JSON kullanılamaz, veri paketleri parça parça gönderilir. 
// consumes = MediaType.MULTIPART_FORM_DATA_VALUE diyerek Spring Boot'a şunu söylüyorsun: 
// "Bu endpointe JSON bekleme, sana parçalı bir form verisi (dosya) gelecek, onu MultipartFile olarak yakala."
 */


//4. String yerine neden nesne dönmeye başladık?
//Dikkat edersen submit, reviewByOidb gibi metotlarda daha önce "Başvuru başarıyla gönderildi." gibi düz String mesajlar dönüyordun. 
//Bunları ApplicationResponse dönecek şekilde güncelledim. Çünkü QA ve yazılım testi süreçlerinde veya ön yüz (React/Vue) geliştirilirken, 
//durumu (status) güncellenmiş nesneyi doğrudan geri dönmek, ön yüzün ekstra bir GET isteği atmasını engeller ve performansı artırır. 


