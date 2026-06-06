package edu.iztech.utms.g02.utms_app.bl.application;

import edu.iztech.utms.g02.utms_app.api.application.dto.*;
import edu.iztech.utms.g02.utms_app.dal.application.entity.*;
import edu.iztech.utms.g02.utms_app.dal.application.repository.*;

import edu.iztech.utms.g02.utms_app.dal.user.entity.Staff; // EKLENDİ: YDYO değişiklik damgası için
import edu.iztech.utms.g02.utms_app.dal.user.entity.Student; // EKLENDİ
import edu.iztech.utms.g02.utms_app.dal.user.repository.StudentRepository; // EKLENDİ

import edu.iztech.utms.g02.utms_app.dal.department.repository.FacultyRepository; // Pair 3: hedef fakülte FK eşlemesi
import edu.iztech.utms.g02.utms_app.dal.department.repository.DepartmentRepository; // Pair 3: hedef bölüm FK eşlemesi

import edu.iztech.utms.g02.utms_app.integration.yoksis.YoksisIntegrationService; // EKLENDİ
import edu.iztech.utms.g02.utms_app.integration.yoksis.dto.YoksisStudentResponse; // EKLENDİ

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;



// EKLENDİ 28.05
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.io.IOException;


/*
// ApplicationService.java
// Tüm iş kurallarının yaşadığı yer. En kritik class.

// - create(): Yeni başvuru oluşturur, durumu DRAFT yapar, veritabanına kaydeder
// - submit(): DRAFT kontrolü yapar → SUBMITTED yapar (DRAFT değilse hata fırlatır)
// - processOidbReview(): ÖİDB kararına göre → YDYO_REVIEW veya OIDB_REJECTED
// - processYdyoReview(): YDYO kararına göre → EVALUATION_QUEUE veya YDYO_REJECTED
// - Her metod: Repository'den veriyi çeker → iş kuralını uygular → kaydeder → Response döner
*/


@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationPeriodRepository applicationPeriodRepository;
    // BURAYI EKLE:
    private final ApplicationHistoryRepository applicationHistoryRepository;

    private final StudentRepository studentRepository; // EKLENDİ
    private final FacultyRepository facultyRepository;       // Pair 3: targetFaculty -> Faculty FK
    private final DepartmentRepository departmentRepository; // Pair 3: targetDepartment -> Department FK
    private final YoksisIntegrationService yoksisIntegrationService; // EKLENDİ
    private final edu.iztech.utms.g02.utms_app.integration.edevlet.EgovDocumentService egovDocumentService; // e-Devlet/ÖSYM mock belge üretimi
    private final edu.iztech.utms.g02.utms_app.bl.notification.NotificationService notificationService; // UC-15: ÖİDB aksiyonlarında öğrenciye bildirim

    //  private final ApplicationMapper applicationMapper; // DTO<->Entity dönüşümleri için --> en aşağıda manuel olarak yapıyoruz, toResponse() metodu ile

    // Şu an başvuru kabul edilen tek akademik yıl. Yeni başvurular yalnızca bu yıla yapılabilir;
    // geçmiş dönemlere ait (ör. 2025-2026) kayıtlar yalnızca görüntüleme amaçlıdır.
    private static final String ACTIVE_ACADEMIC_YEAR = "2026-2027";

    // Yeni başvuruyu ENGELLEMEYEN durumlar. Bunların dışındaki her durum "aktif/devam eden"
    // sayılır ve tek-program kuralı gereği yeni başvuruyu engeller.
    private static final Set<ApplicationStatus> NON_BLOCKING_STATUSES = EnumSet.of(
            ApplicationStatus.DRAFT,
            ApplicationStatus.WITHDRAWN,
            ApplicationStatus.OIDB_REJECTED,
            ApplicationStatus.YDYO_REJECTED,
            ApplicationStatus.FACULTY_BOARD_REJECTED,
            ApplicationStatus.DEAN_REJECTED,
            ApplicationStatus.REJECTED);

    @Transactional
    public ApplicationResponse create(ApplicationCreateRequest req) { // Integer userId, silindi

        // 1. Güvenlik: İsteği atan kullanıcıyı tespit et
        String currentStudentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        // 2. Senin StudentRepository'ni kullanarak veritabanından öğrenciyi çek
        Student currentStudent = studentRepository.findByEmail(currentStudentEmail)
            .orElseThrow(() -> new EntityNotFoundException("Öğrenci bulunamadı."));

        // 0. İŞ KURALI: Akademik yıl yalnızca içinde bulunulan dönem (2026-2027) olabilir.
        if (!ACTIVE_ACADEMIC_YEAR.equals(req.getAcademicYear())) {
            throw new IllegalArgumentException("Başvurular yalnızca " + ACTIVE_ACADEMIC_YEAR + " akademik yılı için yapılabilir.");
        }

        // 1. İŞ KURALI: Bir öğrenci yalnızca TEK bir programa yatay geçiş başvurusu yapabilir.
        // Yalnızca AKTİF (devam eden/onaylı) bir başvuru engel oluşturur. Aşağıdaki durumlar
        // duplicate SAYILMAZ, yeni başvuruya engel değildir:
        //   - DRAFT (yarım kalmış taslak) → yeniden kullanılır,
        //   - WITHDRAWN (geri çekilmiş) → yeniden başvurulabilir,
        //   - *_REJECTED / REJECTED (sonuçlanmış/elenmiş) → ör. geçmiş döneme ait reddedilmiş başvuru.
        boolean hasActiveApplication = applicationRepository
                .existsByStudent_UserIdAndStatusNotIn(currentStudent.getUserId(), NON_BLOCKING_STATUSES);

        if (hasActiveApplication) {
            throw new IllegalArgumentException("Yalnızca bir program için yatay geçiş başvurusu yapabilirsiniz. Devam eden bir başvurunuz bulunduğu için yeni başvuru oluşturamazsınız.");
        }

        // 2. Diğer geçerlilik kontrolleri
        if (!Boolean.TRUE.equals(req.getKvkkAccepted())) {
            throw new IllegalArgumentException("KVKK onayı zorunludur.");
        }

        // 3. Dış Sistem Entegrasyonu: YÖKSİS'ten akademik verileri çek
        YoksisStudentResponse yoksisData = yoksisIntegrationService.fetchAcademicDataByTckn(currentStudent.getTckn());

        // 3b. GPA artık öğrenci tarafından ELLE girilir (4'lük sistem) — YÖKSİS'ten türetilmez.
        //     Önce geçerli aralık (0.00–4.00) kontrol edilir; 2.50 barajı aşağıda
        //     validateApplicationConditions içinde uygulanır.
        Double enteredGpa = req.getGpa();
        if (enteredGpa == null || enteredGpa < 0.0 || enteredGpa > 4.0) {
            throw new IllegalArgumentException("Genel not ortalaması (GPA) 0.00 ile 4.00 arasında girilmelidir.");
        }

        // 3c. Hedef yarıyıl yalnızca 3 veya 5 olabilir (öğrenci sınıfını kendisi seçer:
        //     1. sınıf → 3, 2. sınıf → 5). Eskiden YÖKSİS'teki tamamlanan döneme göre
        //     kısıtlanıyordu; artık seçimi öğrenci yaptığı için o kısıt kaldırıldı.
        //     Taslak yeniden kullanımı da bu kontrolden geçsin diye draft branch'inden ÖNCE.
        Integer targetSemester = req.getSemester();
        if (targetSemester == null || (targetSemester != 3 && targetSemester != 5)) {
            throw new IllegalArgumentException("Yalnızca 3. veya 5. yarıyıla başvuru yapılabilir.");
        }

        // 4. AKADEMİK YETERLİLİK BARAJLARI (GPA + YKS sıralaması) — kayıttan/taslaktan ÖNCE.
        //    Hedef program (Mühendislik/Mimarlık) değiştiğinde sıralama barajı da değişeceği
        //    için bu kontrol taslak yeniden kullanımından ÖNCE çalışmalıdır; aksi halde
        //    önceden onaylı bir taslak farklı bir hedefe taşınarak baraj atlatılabilir.
        validateApplicationConditions(req, enteredGpa, yoksisData);

        // Yarım kalmış bir taslak varsa onu YENİDEN KULLAN (yeni taslak üretme). Hedef program
        // değişmiş olabileceğinden taslağın hedef/dönem alanlarını güncelleriz. Böylece belge
        // yükleme/iptal gibi sebeplerle yarıda kalan akış öğrenciyi kilitlemez.
        Optional<Application> existingDraft = applicationRepository
                .findFirstByStudent_UserIdAndStatus(currentStudent.getUserId(), ApplicationStatus.DRAFT);
        if (existingDraft.isPresent()) {
            Application draft = existingDraft.get();
            draft.setAcademicYear(req.getAcademicYear());
            draft.setSemester(String.valueOf(req.getSemester()));
            draft.setTargetDepartment(req.getTargetDepartment());
            draft.setTargetFaculty(req.getTargetFaculty());
            draft.setGpa(enteredGpa); // öğrencinin elle girdiği güncel GPA
            linkTargetOrgUnits(draft);
            return toResponse(applicationRepository.save(draft));
        }

        // 4. Application objesini oluşturma (Kendi verilerimiz + YÖKSİS verileri + Request verileri harmanlanıyor)
        Application app = Application.builder()
                .student(currentStudent) // İlişkiyi kuruyoruz ?????
                .status(ApplicationStatus.DRAFT) // İlk oluşumda durumu genelde DRAFT (Taslak) olur
                .academicYear(req.getAcademicYear())
                .semester(String.valueOf(req.getSemester())) // hedef yarıyıl (3/5); NOT NULL + unique key parçası
                .targetDepartment(req.getTargetDepartment())
                .targetFaculty(req.getTargetFaculty())

                // YKS verileri artık ÖSYM'den (mock) otomatik gelir; request'ten değil.
                // Öğrenci elle giremediği için kaynak tek ve güvenilir tutulur.
                .sayYksScore(yoksisData.yksScore())
                .sayYksRank(yoksisData.yksRank())

                // YÖKSİS'ten otomatik gelen veriler
                .currentUniversity(yoksisData.currentUniversity())
                .currentFaculty(yoksisData.currentFaculty())
                .currentDepartment(yoksisData.currentDepartment())
                .gpa(enteredGpa) // öğrencinin elle girdiği GPA (4'lük sistem)

                .build();

        // Pair 3: hedef fakülte/bölüm FK'larını bağla (en iyi çaba; eşleşmezse null)
        linkTargetOrgUnits(app);

        // 4. Veritabanına kaydet
        app = applicationRepository.save(app);
        recordStatusChange(app, "Başvuru taslağı oluşturuldu."); // başvuru geçmişine ilk kayıt (DRAFT)

        // 5. e-Devlet/ÖSYM belgelerini (öğrenci belgesi, transkript, YKS sonuç belgesi)
        //    otomatik üret ve başvuruya iliştir — öğrenci bunları elle yüklemez.
        egovDocumentService.generateAndAttach(app, currentStudent, yoksisData);

        // 6. Response olarak dön
        return toResponse(app);
    }

    /**
     * Pair 3: hedef fakülte/bölüm METİNLERİNİ gerçek {@link edu.iztech.utms.g02.utms_app.dal.department.entity.Faculty}/
     * {@link edu.iztech.utms.g02.utms_app.dal.department.entity.Department} FK'larına bağlar.
     *
     * <p>"En iyi çaba": isim birebir eşleşmezse ilgili FK {@code null} bırakılır — herhangi bir
     * DOĞRULAMA yapılmaz (başvuru reddedilmez). Fakülte/bölüm kapsamlı sorgular (dekan listesi,
     * YGK bölüm-bazlı skorlama) bu FK'lar üzerinden çalışır. FK boşsa kayıt o kapsamların dışında kalır.
     */
    private void linkTargetOrgUnits(Application app) {
        app.setFaculty(app.getTargetFaculty() == null ? null
                : facultyRepository.findByName(app.getTargetFaculty()).orElse(null));
        app.setDepartment(app.getTargetDepartment() == null ? null
                : departmentRepository.findByName(app.getTargetDepartment()).orElse(null));
    }

    /**
     * Akademik başvuru koşullarını (iş kuralları) kayıttan ÖNCE doğrular. Bir kural
     * sağlanmazsa {@link IllegalArgumentException} fırlatılır → {@code GlobalExceptionHandler}
     * bunu 400 Bad Request + açıklayıcı mesaj olarak döndürür; başvuru veritabanına HİÇ gitmez.
     *
     * <p>Kurallar:
     * <ol>
     *   <li><b>GPA barajı:</b> 4'lük sistemde &ge; 2.50. {@code gpa4} parametresi öğrencinin
     *       formda ELLE girdiği 4'lük GPA'dır (artık YÖKSİS'ten türetilmez).</li>
     *   <li><b>YKS başarı sıralaması:</b> hedef Mühendislik ise &le; 300.000,
     *       hedef Mimarlık ise &le; 250.000.</li>
     * </ol>
     */
    private void validateApplicationConditions(ApplicationCreateRequest req, double gpa4, YoksisStudentResponse yoksisData) {
        // --- KURAL 1: Not Ortalaması (GPA) — 4'lük sisteme normalize edilmiş değer ---
        if (gpa4 < 2.50) {
            throw new IllegalArgumentException(String.format(Locale.ROOT,
                    "Başvurunuz reddedildi: Genel not ortalamanız (%.2f/4.00), yatay geçiş barajı olan 2.50/4.00 değerinin altındadır.",
                    gpa4));
        }

        // --- KURAL 2: ÖSYS/YKS Başarı Sıralaması (hedef programa göre) ---
        Integer rankLimit = resolveYksRankLimit(req.getTargetFaculty(), req.getTargetDepartment());
        if (rankLimit != null) {
            Integer rank = yoksisData.yksRank();
            if (rank == null || rank > rankLimit) {
                throw new IllegalArgumentException(String.format(Locale.ROOT,
                        "Başvurunuz reddedildi: Seçtiğiniz program için YKS başarı sıralamanız en fazla %,d olmalıdır (mevcut sıralamanız: %s).",
                        rankLimit, (rank == null ? "bilinmiyor" : String.format(Locale.ROOT, "%,d", rank))));
            }
        }
    }

    /**
     * Hedef fakülte/bölüm metninden YKS sıralama barajını çözer:
     * <ul>
     *   <li>"Mühendislik" içeren hedefler &rarr; 300.000</li>
     *   <li>"Mimarlık" içeren hedefler &rarr; 250.000</li>
     *   <li>diğerleri &rarr; {@code null} (bu kural uygulanmaz)</li>
     * </ul>
     */
    private Integer resolveYksRankLimit(String targetFaculty, String targetDepartment) {
        String hay = ((targetFaculty == null ? "" : targetFaculty) + " "
                + (targetDepartment == null ? "" : targetDepartment)).toLowerCase(Locale.ROOT);
        if (hay.contains("mühendis")) return 300_000;   // Mühendislik programları
        if (hay.contains("mimar"))    return 250_000;   // Mimarlık programları
        return null;
    }

    @Transactional
    public ApplicationResponse submit(Integer applicationId) { //, Integer userId silindi

        // Sadece başvuru id'si yeterli, kullanıcının kendi başvurusu olup olmadığını kontrol edelim
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        
        verifyOwnership(app); // Helper metot ile güvenlik kontrolü

        if (app.getStatus() != ApplicationStatus.DRAFT && app.getStatus() != ApplicationStatus.REVISION_REQUESTED) { // REVISION_REQUESTED ekledik, böylece öğrenci düzeltme yapıp tekrar gönderebilir
            throw new IllegalStateException("Sadece DRAFT veya REVISION_REQUESTED durumundaki başvurular gönderilebilir.");
        }
        
        app.setStatus(ApplicationStatus.SUBMITTED);
        // Başvuru tarihi = öğrencinin başvuruyu İLK gönderdiği an. REVISION_REQUESTED'tan
        // sonra tekrar gönderimde orijinal tarihi korumak için yalnızca boşken atanır.
        if (app.getSubmissionDate() == null) {
            app.setSubmissionDate(LocalDate.now());
        }
        app = applicationRepository.save(app);
        recordStatusChange(app, "Başvuru onaya gönderildi."); // geçmişe SUBMITTED kaydı

        return toResponse(app);
    }

    // --- DIŞARIYA AÇIK TEK METOT (DISPATCHER) ---
    @Transactional
    public ApplicationResponse processDynamicOidbReview(Integer applicationId, OidbReviewRequest req) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Başvuru bulunamadı."));

        // Başvurunun o anki statüsüne göre doğru iş akışına yönlendir (Dynamic Routing)
        switch (app.getStatus()) {
            case SUBMITTED:
            case REVISION_REQUESTED:
                return processOidbReviewAfterSubmission(app, req);

            case YDYO_ACCEPTED:
            case YDYO_REJECTED:
                // 2. aşamada req içindeki 'approved' alanı true ise Dekanlığa ilet, false ise Reddet
                boolean forwardToDean = Boolean.TRUE.equals(req.isApproved());
                return processOidbPostYdyoReview(app, forwardToDean, req);

            default:
                throw new IllegalStateException("Başvuru şu an OİDB'nin işlem yapabileceği bir statüde değil. Güncel Statü: " + app.getStatus());
        }
    }

    // --------------------------------------------------------
    // GİZLİ (PRIVATE) İŞ AKIŞI METOTLARI
    // --------------------------------------------------------

    // AŞAMA 1: İlk Evrak Kontrolü
    private ApplicationResponse processOidbReviewAfterSubmission(Application app, OidbReviewRequest req) {
        if (req.isRequestRevision()) {
            if (app.isRevisionRequestedBefore()) {
                throw new IllegalStateException("Bir başvuru için sadece bir kez belge güncellemesi istenebilir.");
            }
            app.setStatus(ApplicationStatus.REVISION_REQUESTED);
            app.setRevisionRequestedBefore(true);
            // Memurun seçtiği hatalı belge(ler) ve düzeltme notunu kaydet → öğrenci ekranı bunları kullanır.
            // Birden fazla belge tek kolonda CSV olarak saklanır ("TRANSCRIPT,LANGUAGE_CERT").
            app.setRequestedDocumentType(joinRequestedDocumentTypes(req));
            app.setRevisionNotes(req.getRevisionNotes());
        } else if (Boolean.TRUE.equals(req.isApproved())) {
            app.setStatus(ApplicationStatus.YDYO_REVIEW); 
        } else {
            app.setStatus(ApplicationStatus.OIDB_REJECTED); 
        }

        app.setOidbApproved(req.isApproved());
        app.setOidbNotes(req.getNotes());
        app.setOidbReviewedBy(req.getReviewer());
        app.setOidbReviewedDate(LocalDateTime.now());

        Application saved = applicationRepository.save(app);
        recordStatusChange(saved, req.isRequestRevision() ? req.getRevisionNotes() : req.getNotes()); // EKLENDİ

        // UC-15: ÖİDB ön inceleme sonucunu öğrenciye bildir (Güncel Durum ile birebir).
        switch (saved.getStatus()) {
            case REVISION_REQUESTED -> notifyStudent(saved, "Belge Güncellemesi Gerekiyor",
                    saved.getRevisionNotes() != null && !saved.getRevisionNotes().isBlank()
                            ? saved.getRevisionNotes()
                            : "Başvurunuzdaki belgelerin güncellenmesi istenmektedir. Lütfen başvuru detayından ilgili belgeleri yeniden yükleyin.");
            case YDYO_REVIEW -> notifyStudent(saved, "Ön İnceleme Tamamlandı",
                    "Başvurunuz Öğrenci İşleri ön incelemesinden başarıyla geçti ve değerlendirme sürecine alındı.");
            case OIDB_REJECTED -> notifyStudent(saved, "Başvurunuz Reddedildi",
                    saved.getOidbNotes() != null && !saved.getOidbNotes().isBlank()
                            ? saved.getOidbNotes()
                            : "Başvurunuz Öğrenci İşleri tarafından reddedilmiştir.");
            default -> { /* bildirim yok */ }
        }

        return toResponse(saved);
    }

    // AŞAMA 2: YDYO Sonrası Karar
    private ApplicationResponse processOidbPostYdyoReview(Application app, boolean forwardToDean, OidbReviewRequest req) {
        if (forwardToDean && app.getStatus() == ApplicationStatus.YDYO_ACCEPTED) {
            // Pair 3 devir noktası: Dekanlık Ofisi'ne gönderilir ('Akademik İnceleme Bekliyor').
            // Dekan 'YGK'ya İlet' ile EVALUATION_QUEUE'ya taşır; YGK score-all oradan alır (TC-10.0).
            app.setStatus(ApplicationStatus.DEAN_OFFICE_REVIEW);
        } else {
            app.setStatus(ApplicationStatus.REJECTED); 
        }
        
        // Memur bu aşamada da not eklemek isteyebilir

        app.setOidbApproved(req.isApproved());
        app.setOidbNotes(req.getNotes());
        app.setOidbReviewedBy(req.getReviewer());
        app.setOidbReviewedDate(LocalDateTime.now());

        Application saved = applicationRepository.save(app);
        recordStatusChange(saved, req.getNotes()); // EKLENDİ

        // UC-15: YDYO sonucu öğrenciye ANCAK burada (ÖİDB post-YDYO işlemi) yüzeye çıkar.
        if (saved.getStatus() == ApplicationStatus.DEAN_OFFICE_REVIEW) {
            // DEAN_OFFICE_REVIEW yalnızca YDYO_ACCEPTED'tan ilerletilince oluşur.
            // ydyoApproved==true → belge muafiyeti; false → sınavı geçti.
            String msg = Boolean.TRUE.equals(saved.getYdyoApproved())
                    ? "Yabancı dil şartından muaf tutuldunuz. Başvurunuz değerlendirme sürecine alındı."
                    : "Yabancı dil sınavını başarıyla geçtiniz. Başvurunuz değerlendirme sürecine alındı.";
            notifyStudent(saved, "Yabancı Dil Şartı Tamamlandı", msg);
        } else if (saved.getStatus() == ApplicationStatus.REJECTED) {
            notifyStudent(saved, "Başvurunuz Reddedildi",
                    saved.getOidbNotes() != null && !saved.getOidbNotes().isBlank()
                            ? saved.getOidbNotes()
                            : "Başvurunuz Öğrenci İşleri tarafından reddedilmiştir.");
        }

        return toResponse(saved);
    }




    // --------------------------------------------------------
    // YDYO 1. AŞAMA: EVRAK KONTROLÜ
    // --------------------------------------------------------

    @Transactional
    public ApplicationResponse processYdyoReview(Integer applicationId, YdyoReviewRequest req) {
        
        Application app = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        
        
        // YDYO yalnızca kendi aşamasındaki kayıtlarda işlem yapar. Kesin karara bağlı
        // (ACCEPTED/REJECTED) bir kaydı YENİDEN değerlendirmeye izin verilir, ancak bu
        // "değişiklik" olarak damgalanır (yanlışlıkla yapılan değişiklik koruması, UC-7).
        if (!YDYO_EDITABLE_STATUSES.contains(app.getStatus())) {
            throw new IllegalStateException("Bu başvuru YDYO'nun işlem yapabileceği bir aşamada değil. Güncel Statü: " + app.getStatus());
        }
        boolean wasDecided = isYdyoDecided(app);

        // 1. Durum: Öğrencinin belgesi yetersiz, sınava girecek
        if (Boolean.TRUE.equals(req.getRequiresExam())) {
            app.setStatus(ApplicationStatus.YDYO_EXAM_PENDING);
            app.setYdyoExamScore(null);   // yeniden sınava → önceki sonuç (varsa) sıfırlanır
            app.setYdyoResultStatus(null); // henüz kesin karar yok
        }
        // 2. Durum: Belgesi yeterli (Muaf)
        else if (Boolean.TRUE.equals(req.isApproved())) {
            app.setStatus(ApplicationStatus.YDYO_ACCEPTED); //  EVALUATION_QUEUE yerine YDYO_ACCEPTED olmalı ??
            app.setYdyoExamScore(null);   // muaf → sınav notu anlamsız, temizlenir
            app.setYdyoResultStatus(ApplicationStatus.YDYO_ACCEPTED); // YDYO kararını dondur
        }
        // 3. Durum: Belge reddedildi (onaysız + sınava da girmeyecek) → eleme
        else {
            app.setStatus(ApplicationStatus.YDYO_REJECTED);
            app.setYdyoExamScore(null);
            app.setYdyoResultStatus(ApplicationStatus.YDYO_REJECTED); // YDYO kararını dondur
        }


        app.setYdyoApproved(req.isApproved());
        app.setYdyoNotes(req.getNotes());
        app.setYdyoReviewedBy(req.getReviewer());
        app.setYdyoReviewedDate(LocalDateTime.now());
        stampYdyoModification(app, wasDecided, req.getReviewer());

        app = applicationRepository.save(app);
        recordStatusChange(app, req.getNotes()); // EKLENDİ
        return toResponse(app);

    }

    // --------------------------------------------------------
    // YDYO 2. AŞAMA (TEKİL): Bir öğrencinin sınav sonucunu gir
    //  - Toplu CSV yolundan farklı olarak detay ekranından tek tek girilir.
    //  - Geç/kaldı kararı manuel 'passed' ile gelir (eşik UI'da uygulanır).
    // --------------------------------------------------------
    @Transactional
    @PreAuthorize("hasAnyRole('YDYO', 'ROLE_YDYO')")
    public ApplicationResponse enterYdyoExamResult(Integer applicationId, YdyoExamResultRequest req) {
        Application app = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Başvuru bulunamadı."));

        // Sınav sonucu yalnız sınava yönlendirilmiş (EXAM_PENDING) ya da kesin karara
        // bağlı (ACCEPTED/REJECTED → yeniden değerlendirme) kayıtlara girilebilir.
        if (app.getStatus() != ApplicationStatus.YDYO_EXAM_PENDING && !isYdyoDecided(app)) {
            throw new IllegalStateException("Bu başvuru için sınav sonucu girilebilecek bir aşamada değil. Güncel Statü: " + app.getStatus());
        }
        boolean wasDecided = isYdyoDecided(app);

        app.setYdyoExamScore(req.getExamScore());
        app.setYdyoNotes(req.getNotes());
        app.setYdyoReviewedBy(req.getReviewer());
        app.setYdyoReviewedDate(LocalDateTime.now());

        // Sınav yolu → belge muafiyeti yoktur. ydyoApproved=false yapılır ki türetilen
        // alanlar (requiresExam=true, "Onaylanmadı") kayıtla TUTARLI kalsın — eski hatada
        // bu alanlar eski kalıp rozetler çelişiyordu.
        app.setYdyoApproved(false);
        if (Boolean.TRUE.equals(req.getPassed())) {
            app.setStatus(ApplicationStatus.YDYO_ACCEPTED);   // sınavı geçti → muaf
            app.setYdyoResultStatus(ApplicationStatus.YDYO_ACCEPTED); // YDYO kararını dondur
        } else {
            app.setStatus(ApplicationStatus.YDYO_REJECTED);   // sınavdan kaldı
            app.setYdyoResultStatus(ApplicationStatus.YDYO_REJECTED); // YDYO kararını dondur
        }

        app = applicationRepository.save(app); // return satırından bir önce kaydetmeyi ayır
        recordStatusChange(app, req.getNotes()); // EKLENDİ

        stampYdyoModification(app, wasDecided, req.getReviewer());
        return toResponse(app);
    }



    // --------------------------------------------------------
    // YDYO: "SONUÇLARI ÖİDB'YE İLET"
    //  - YDYO değerlendirmesini bitirip kararları (ACCEPTED/REJECTED) resmen ÖİDB'ye
    //    iletir. İletilene dek ÖİDB bu kararları GÖREMEZ (panelde "YDYO'da" maskelenir).
    //  - Bayrak DB'de kalıcıdır → yenileme/oturum kapatma sonrası geri dönmez.
    //  - İŞ KURALI: listede hâlâ tamamlanmamış (REVIEW/EXAM_PENDING) kayıt varsa iletilemez.
    // --------------------------------------------------------
    @Transactional
    @PreAuthorize("hasAnyRole('YDYO', 'ROLE_YDYO')")
    public int forwardYdyoResultsToOidb() {
        // Hâlâ değerlendirme süren kayıt varken iletime izin verme (frontend guard'ının backend karşılığı).
        boolean hasPending = applicationRepository.existsByStatusIn(
                List.of(ApplicationStatus.YDYO_REVIEW, ApplicationStatus.YDYO_EXAM_PENDING));
        if (hasPending) {
            throw new IllegalStateException("Tüm öğrenci kayıtları tamamlanmadan liste ÖİDB'ye iletilemez.");
        }

        // Karara bağlanmış ama henüz iletilmemiş kayıtları işaretle.
        List<Application> toForward = applicationRepository.findByStatusInAndYdyoForwardedToOidb(
                List.of(ApplicationStatus.YDYO_ACCEPTED, ApplicationStatus.YDYO_REJECTED), false);

        for (Application app : toForward) {
            app.setYdyoForwardedToOidb(true);
        }
        applicationRepository.saveAll(toForward);

        return toForward.size();
    }

    // --------------------------------------------------------
    // YDYO 2. AŞAMA: TOPLU CSV İLE SINAV SONUCU YÜKLEME
    // --------------------------------------------------------
    @Transactional
    @PreAuthorize("hasAnyRole('YDYO', 'ROLE_YDYO')")
    public int uploadYdyoExamResultsCsv(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Yüklenen CSV dosyası boş olamaz.");
        }

        // YENİ EKLENEN: Sadece .csv uzantılı dosyalara izin ver
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Geçersiz dosya formatı. Lütfen sadece .csv uzantılı dosya yükleyin.");
        }

        int processedCount = 0; // Kaç öğrencinin güncellendiğini tutmak için

        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            
            String line;
            boolean isFirstLine = true; // Başlık (Header) satırını atlamak için

            while ((line = br.readLine()) != null) {
                // Türkiye bölgesel ayarlarında Excel CSV'leri noktalı virgül (;) ile, 
                // İngilizce sistemler virgül (,) ile böler. İkisine de hazırlıklıyız.
                String[] columns = line.split("[,;]"); 

                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // "Ad Soyad, E-posta, Sınav Sonucu" yazan ilk satırı atla
                }

                // Beklenen format: [0] Ad Soyad, [1] E-posta, [2] Sınav Sonucu
                if (columns.length < 3) continue; // Eksik veri olan satırları atla

                String email = columns[1].trim();
                String scoreStr = columns[2].trim();

                try {
                    Double score = Double.parseDouble(scoreStr);
                    
                    // Öğrencinin "YDYO_EXAM_PENDING" (Sınav Bekliyor) statüsündeki başvurusunu bul
                    List<Application> pendingApps = applicationRepository.findByStudent_EmailAndStatus(email, ApplicationStatus.YDYO_EXAM_PENDING);
                    
                    if (!pendingApps.isEmpty()) {
                        Application app = pendingApps.get(0); // Öğrencinin o dönemki aktif başvurusunu al
                        
                        app.setYdyoExamScore(score);
                        app.setYdyoReviewedDate(LocalDateTime.now());
                        
                        // İŞ KURALI: 60 ve üzeri Muaf (Kabul), altı ise Red
                        if (score >= 60.0) {
                            app.setStatus(ApplicationStatus.YDYO_ACCEPTED);
                            app.setYdyoResultStatus(ApplicationStatus.YDYO_ACCEPTED); // YDYO kararını dondur
                        } else {
                            app.setStatus(ApplicationStatus.YDYO_REJECTED);
                            app.setYdyoResultStatus(ApplicationStatus.YDYO_REJECTED); // YDYO kararını dondur
                        }
                        
                        applicationRepository.save(app);
                        recordStatusChange(app, "YDYO sınav sonucu girildi."); // EKLENDİ
                        processedCount++;
                    }

                } catch (NumberFormatException e) {
                    // Not kısmı sayı değilse (örn: "Girmedi" yazıyorsa) bu satırı güvenle atla
                    System.err.println("Geçersiz not formatı atlandı: " + scoreStr + " (Email: " + email + ")");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("CSV dosyası işlenirken sistemsel bir hata oluştu: " + e.getMessage());
        }

        return processedCount; // Başarıyla güncellenen öğrenci sayısını döndürüyoruz
    }

    // ==========================================
    // YENİ EKLENEN 3 METOT (GET ALL, GET BY ID, UPLOAD)
    // ==========================================

    //public List<ApplicationResponse> getAllApplications() {
        //return getAllApplications(null);
    //}

    @Transactional(readOnly = true) // toResponse içinde lazy documents erişimi için
    public Page<ApplicationResponse> getAllApplications(ApplicationStatus status, int page, int size) { // eklendi 29.05 : ApplicationStatus status, int page, int size
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // JWT'den gelen getName() metodu kullanıcının E-POSTA adresini döner (Örn: busra@std.iztech.edu.tr)
        String currentUserEmail = authentication.getName(); 

        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT") || a.getAuthority().equals("STUDENT"));

        //eklendi 29.05
        Pageable pageable = PageRequest.of(page, size);

        if (isStudent) {
            // E-postadan ID'yi buluyoruz
            Student currentStudent = studentRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı."));

            if (status == null) {
                return applicationRepository.findByStudent_UserId(currentStudent.getUserId(),pageable).map(this::toResponse);
            }

            return applicationRepository.findByStudent_UserIdAndStatus(currentStudent.getUserId(), status, pageable).map(this::toResponse);

        } else {
            // Öğrenci değilse (OIDB, YDYO, FACULTY, DEAN vs.) herkesi görebilir
            if (status == null) {
                return applicationRepository.findAll(pageable).map(this::toResponse);
            }

            return applicationRepository.findByStatus(status, pageable).map(this::toResponse);
        }

    }

    @Transactional(readOnly = true) // toResponse içinde lazy documents erişimi için
    public ApplicationResponse getApplicationById(Integer id) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Başvuru bulunamadı. ID: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //String currentUsername = authentication.getName();
        
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT") || a.getAuthority().equals("STUDENT"));

        // Güvenlik: Öğrenci sadece kendi başvurusunu görebilir. String.valueOf() ile tip uyuşmazlığını önlüyoruz.
        if (isStudent) {
            verifyOwnership(app);
        }

        return toResponse(app);
    }


    @Transactional
    public ApplicationResponse withdrawApplication(Integer applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Başvuru bulunamadı."));

        // 1. GÜVENLİK: Sadece başvurunun sahibi iptal edebilir
        verifyOwnership(app);

        // 2. ZAMAN KONTROLÜ (Tek ve kesin kural)
        if (!isApplicationPeriodActive()) {
            throw new IllegalStateException("Başvuru dönemi sona erdiği için başvurunuzu artık geri çekemezsiniz.");
        }

        // Sistemin patlamaması için eklenebilecek tek minik statü kontrolü (Opsiyonel)
        if (app.getStatus() == ApplicationStatus.WITHDRAWN) {
             throw new IllegalStateException("Başvuru zaten geri çekilmiş.");
        }

        // 3. STATÜYÜ GÜNCELLE VE KAYDET
        app.setStatus(ApplicationStatus.WITHDRAWN);
        recordStatusChange(app, "Öğrenci başvuruyu geri çekti."); // EKLENDİ
        app = applicationRepository.save(app);

        return toResponse(app);
    }


    // --------------------------------------------------------
    // ÖİDB: DOĞRUDAN RED
    // --------------------------------------------------------
    // Memur, kriterleri sağlamayan veya evrakları hâlâ geçersiz olan bir başvuruyu
    // güncelleme/YDYO akışına sokmadan doğrudan REJECTED yapabilir. (POST /{id}/reject)
    @Transactional
    public ApplicationResponse rejectApplication(Integer applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Başvuru bulunamadı. ID: " + applicationId));

        // Nihai karara bağlanmış ya da geri çekilmiş başvurular tekrar reddedilemez.
        Set<ApplicationStatus> notRejectable = EnumSet.of(
                ApplicationStatus.APPROVED,
                ApplicationStatus.ACCEPTED,
                ApplicationStatus.REJECTED,
                ApplicationStatus.WITHDRAWN);
        if (notRejectable.contains(app.getStatus())) {
            throw new IllegalStateException("Bu başvuru mevcut statüsünde reddedilemez. Güncel Statü: " + app.getStatus());
        }

        app.setStatus(ApplicationStatus.REJECTED);
        app.setOidbReviewedDate(LocalDateTime.now());

        Application saved = applicationRepository.save(app);
        recordStatusChange(saved, "ÖİDB tarafından doğrudan reddedildi."); // EKLENDİ

        // UC-15: Doğrudan red sonucunu öğrenciye bildir.
        notifyStudent(saved, "Başvurunuz Reddedildi",
                saved.getOidbNotes() != null && !saved.getOidbNotes().isBlank()
                        ? saved.getOidbNotes()
                        : "Başvurunuz Öğrenci İşleri tarafından reddedilmiştir.");

        return toResponse(saved);
    }


    // UC-15: Başvurunun sahibi öğrenciye bildirim üretir. Yalnızca ÖİDB aksiyonlarında
    // çağrılır; öğrenci "Güncel Durum" kartında gördüğü ÖİDB sonucunu burada da görür.
    private void notifyStudent(Application app, String title, String message) {
        if (app.getStudent() != null) {
            notificationService.create(app.getStudent().getUserId(), title, message);
        }
    }

    // --- ZAMAN KONTROLÜ İÇİN YARDIMCI METOT ---
    private boolean isApplicationPeriodActive() {
        // Veritabanından "aktif" olarak işaretlenmiş başvuru dönemini çek
        Optional<ApplicationPeriod> activePeriodOpt = applicationPeriodRepository.findByActiveTrue();

        // Eğer veritabanında aktif bir dönem tanımlanmamışsa, kimse işlem yapamaz (false döner)
        if (activePeriodOpt.isEmpty()) {
            return false;
        }

        ApplicationPeriod currentPeriod = activePeriodOpt.get();
        LocalDateTime now = LocalDateTime.now();

        // Şu anki zaman, başlangıç tarihinden SONRA ve bitiş tarihinden ÖNCE ise true döner
        return !now.isBefore(currentPeriod.getStartDate()) && !now.isAfter(currentPeriod.getEndDate());
    }

    // --- HELPER METHODS ---

    // YDYO'nun işlem yapabileceği statüler. ACCEPTED/REJECTED dahil → kesin karar
    // sonrası YENİDEN değerlendirmeye izin verir (değişiklik damgalanır).
    private static final Set<ApplicationStatus> YDYO_EDITABLE_STATUSES = EnumSet.of(
            ApplicationStatus.YDYO_REVIEW,
            ApplicationStatus.YDYO_EXAM_PENDING,
            ApplicationStatus.YDYO_ACCEPTED,
            ApplicationStatus.YDYO_REJECTED);

    // Kayıt zaten KESİN bir YDYO kararına bağlı mı? (ACCEPTED/REJECTED)
    private boolean isYdyoDecided(Application app) {
        return app.getStatus() == ApplicationStatus.YDYO_ACCEPTED
                || app.getStatus() == ApplicationStatus.YDYO_REJECTED;
    }

    // Kesin karara bağlı kayıt yeniden değerlendirildiyse "değişiklik yapılmıştır" damgası.
    private void stampYdyoModification(Application app, boolean wasDecided, Staff reviewer) {
        if (!wasDecided) return;
        app.setYdyoDecisionModified(true);
        app.setYdyoModifiedBy(reviewer);
        app.setYdyoModifiedDate(LocalDateTime.now());
    }

    // DRY (Don't Repeat Yourself) prensibi için sahiplik kontrolünü tek bir yere aldık
    private void verifyOwnership(Application app) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // E-posta ile veritabanından kullanıcıyı bul
        Student student = studentRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı."));

        // Veritabanındaki Student ID'si ile Başvuru üzerindeki Student ID eşleşiyor mu?
        if (!app.getStudent().getUserId().equals(student.getUserId())) { 
            throw new AccessDeniedException("Bu başvuru üzerinde işlem yapma yetkiniz bulunmuyor.");
        }
    }

    private ApplicationResponse toResponse(Application app) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(app.getApplicationId());
        //response.setStudentId(app.getStudent().getUserId()); //
        response.setStatus(app.getStatus());
        response.setAcademicYear(app.getAcademicYear());
        response.setSemester(app.getSemester());
        response.setTargetFaculty(app.getTargetFaculty());
        response.setTargetDepartment(app.getTargetDepartment());
        response.setCurrentUniversity(app.getCurrentUniversity());
        response.setCurrentFaculty(app.getCurrentFaculty());
        response.setCurrentDepartment(app.getCurrentDepartment());
        response.setGpa(app.getGpa());
        response.setSayYksScore(app.getSayYksScore());
        response.setSayYksRank(app.getSayYksRank());
        response.setSubmissionDate(app.getSubmissionDate());

        // Öğrenci kimlik/iletişim (YDYO paneli için) — student @ManyToOne EAGER, güvenli
        Student student = app.getStudent();
        if (student != null) {
            response.setStudentName(buildFullName(student));
            response.setTckn(student.getTckn());
            response.setEmail(student.getEmail());
            response.setPhoneNumber(student.getPhoneNumber());
            response.setDateOfBirth(student.getDateOfBirth());
        }

        // ÖİDB inceleme detayları
        response.setOidbApproved(app.getOidbApproved());
        response.setOidbNotes(app.getOidbNotes());
        response.setOidbReviewedBy(app.getOidbReviewedBy() != null ? app.getOidbReviewedBy().getUserId() : null);
        response.setOidbReviewedDate(app.getOidbReviewedDate());

        // Düzeltme isteği detayları (hangi belge(ler) + not) → öğrenci ve ÖİDB ekranları kullanır
        response.setRequestedDocumentTypes(splitRequestedDocumentTypes(app.getRequestedDocumentType()));
        response.setRevisionNotes(app.getRevisionNotes());
        response.setRevisionRequestedBefore(app.isRevisionRequestedBefore());

        // YDYO inceleme detayları
        response.setYdyoApproved(app.getYdyoApproved());
        response.setYdyoNotes(app.getYdyoNotes());
        response.setYdyoReviewedBy(app.getYdyoReviewedBy() != null ? app.getYdyoReviewedBy().getUserId() : null);
        response.setYdyoReviewedDate(app.getYdyoReviewedDate());
        response.setExamScore(app.getYdyoExamScore());

        // YDYO kararı kesinleştikten sonra değiştirildiyse → "değişiklik yapılmıştır" işareti
        response.setModified(app.isYdyoDecisionModified());
        response.setModifiedBy(app.getYdyoModifiedBy() != null ? app.getYdyoModifiedBy().getUserId() : null);
        response.setModifiedAt(app.getYdyoModifiedDate());

        // YDYO sonuçları ÖİDB'ye iletildi mi? (ÖİDB görünürlüğünü + YDYO kilidini belirler)
        response.setYdyoForwardedToOidb(app.isYdyoForwardedToOidb());

        // Türetilen YDYO alanları (entity'de saklanmıyor):
        //   ydyoApproved == true  → belge onaylı, muaf, sınav gerekmez (requiresExam=false)
        //   ydyoApproved == false → sınava yönlendirildi (requiresExam=true)
        //   ydyoApproved == null  → henüz değerlendirilmedi (REVIEW)
        Boolean ydyoApproved = app.getYdyoApproved();
        response.setRequiresExam(ydyoApproved == null ? null : !ydyoApproved);

        // YDYO'nun dondurulmuş kesin kararı → ÖİDB/fakülte hattına ilerlese de YDYO paneli
        // bu kararla gösterir/kilitler. Frontend display status'ı bundan türetir.
        response.setYdyoResultStatus(app.getYdyoResultStatus());

        // examPassed türetimi CANLI status yerine "etkin YDYO statüsü"nden yapılır: kayıt
        // YDYO_* aşamasından çıkmışsa dondurulmuş ydyoResultStatus kullanılır → statü
        // ilerlese de "Sınav Başarılı/Başarısız" rozeti kaybolmaz.
        ApplicationStatus effectiveYdyoStatus = app.getStatus();
        if (effectiveYdyoStatus != ApplicationStatus.YDYO_ACCEPTED
                && effectiveYdyoStatus != ApplicationStatus.YDYO_REJECTED
                && app.getYdyoResultStatus() != null) {
            effectiveYdyoStatus = app.getYdyoResultStatus();
        }

        // examPassed: REJECTED → kaldı (false); sınav yoluyla ACCEPTED (belge onaysız) → geçti (true);
        // diğer durumlarda (REVIEW, EXAM_PENDING, muafiyetle ACCEPTED) belirsiz → null
        Boolean examPassed = null;
        if (effectiveYdyoStatus == ApplicationStatus.YDYO_REJECTED) {
            examPassed = false;
        } else if (effectiveYdyoStatus == ApplicationStatus.YDYO_ACCEPTED && Boolean.FALSE.equals(ydyoApproved)) {
            examPassed = true;
        }
        response.setExamPassed(examPassed);

        // Belgeler — aktif olanların hafif özeti
        if (app.getDocuments() != null) {
            response.setDocuments(app.getDocuments().stream()
                    .filter(Document::isActive)
                    .map(d -> ApplicationResponse.DocumentSummary.builder()
                            .documentId(d.getDocumentId())
                            .documentType(d.getDocumentType())
                            .fileName(d.getFileName())
                            .build())
                    .collect(Collectors.toList()));
        }
        // --- EKLENDİ: Frontend Timeline'ı için Geçmiş Listesi ---
        List<ApplicationHistory> historyList = applicationHistoryRepository.findByApplicationIdOrderByChangedAtDesc(app.getApplicationId());
        if (!historyList.isEmpty()) {
            response.setStatusHistory(historyList.stream()
                    .map(h -> ApplicationResponse.StatusHistorySummary.builder()
                            .status(h.getStatus())
                            .changedAt(h.getChangedAt())
                            .note(h.getNote())
                            .build())
                    .collect(Collectors.toList()));
        }

        return response;
    }

    // Düzeltme istenen belge tiplerini CSV'ye çevir. Yeni istemci listeyi (requestedDocumentTypes)
    // gönderir; eski istemci tek alanı (requestedDocumentType) gönderebilir — ikisini de destekleriz.
    private String joinRequestedDocumentTypes(OidbReviewRequest req) {
        List<String> types = req.getRequestedDocumentTypes();
        if (types != null && !types.isEmpty()) {
            return types.stream()
                    .filter(t -> t != null && !t.isBlank())
                    .map(String::trim)
                    .collect(Collectors.joining(","));
        }
        return req.getRequestedDocumentType(); // geriye dönük tek belge
    }

    // CSV olarak saklanan belge tiplerini listeye ayır (boşsa boş liste).
    private List<String> splitRequestedDocumentTypes(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // Ad + (varsa) ikinci ad + soyad → tek görünen ad
    private String buildFullName(Student student) {
        StringBuilder sb = new StringBuilder();
        if (student.getFirstName() != null) sb.append(student.getFirstName());
        if (student.getMiddleName() != null && !student.getMiddleName().isBlank()) {
            sb.append(' ').append(student.getMiddleName());
        }
        if (student.getLastName() != null) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(student.getLastName());
        }
        return sb.toString().trim();
    }

    // --- EKLENDİ: Başvuru Geçmişi Kaydedici ---
    private void recordStatusChange(Application app, String note) {
        ApplicationHistory history = ApplicationHistory.builder()
                .applicationId(app.getApplicationId())
                .status(app.getStatus().name())
                .changedAt(LocalDateTime.now())
                .note(note)
                .build();
        applicationHistoryRepository.save(history);
    }
}