package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.evaluation.dto.PublishedResultResponse;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.EvaluationResult;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.PublishedResult;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.EvaluationResultRepository;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.PublishedResultRepository;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Student;
import edu.iztech.utms.g02.utms_app.dal.user.entity.User;
import edu.iztech.utms.g02.utms_app.dal.user.repository.UserRepository;
import edu.iztech.utms.g02.utms_app.bl.notification.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ÖİDB sonuç yayınlama ve sonuç görüntüleme.
 *
 * <p>Sonuçlar yayınlanana (published_results satırı oluşana) kadar öğrenciye görünmez.
 * Dekan, Fakülte Kurulu kararını ÖİDB'ye iletince: kabul → {@code OIDB_FINAL_REVIEW}, red → {@code REJECTED}.
 * ÖİDB yayınlayınca: {@code OIDB_FINAL_REVIEW → ACCEPTED} ve {@code REJECTED} → red kaydı
 * (öğrenci her iki durumda da bilgilendirilir — TC-10.4).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ResultService {

    private final ApplicationRepository applicationRepository;
    private final PublishedResultRepository publishedResultRepository;
    private final EvaluationResultRepository evaluationResultRepository; // asil/yedek için bölüm-içi ranking
    private final UserRepository userRepository;
    private final NotificationService notificationService; // TC-6.0: yayında öğrenci bildirimi (in-app)

    /**
     * Nihai aşamadaki başvuruları yayınlar. Halihazırda yayınlanmış başvurular atlanır
     * (idempotent). Kabul edilenlerin statüsü ACCEPTED yapılır.
     *
     * @return yayınlanan (yeni) sonuç sayısı
     */
    @Transactional
    public int publishResults() {
        User publisher = resolveCurrentUser();

        // TC-6.2: Bekleyen Fakülte Kurulu kararı ('Fakülte Kurulu Kararı Bekleniyor') varken yayın engellenir.
        if (!findByStatus(ApplicationStatus.FACULTY_BOARD_REVIEW).isEmpty()) {
            throw new IllegalStateException(
                    "Tüm Fakülte Kurulu kararları tamamlanmadan sonuçlar yayınlanamaz.");
        }

        int count = 0;

        // Asil/Yedek: bölüm bazında kabul edilenleri ranking'e göre sıralayıp ata (Department.quota).
        Map<Integer, String> listTypeByApp = computeListTypes();

        // Kabul edilenler: Dekanlık, Fakülte Kurulu'nca kabul edilen başvuruyu ÖİDB'ye iletince
        // statü OIDB_FINAL_REVIEW olur; ÖİDB yayınlayınca OIDB_FINAL_REVIEW → ACCEPTED.
        for (Application app : findByStatus(ApplicationStatus.OIDB_FINAL_REVIEW)) {
            if (publishedResultRepository.existsByApplication_ApplicationId(app.getApplicationId())) {
                continue;
            }
            publishedResultRepository.save(PublishedResult.builder()
                    .application(app)
                    .finalDecision("ACCEPTED")
                    .listType(listTypeByApp.get(app.getApplicationId()))
                    .publishedBy(publisher)
                    .build());
            app.setStatus(ApplicationStatus.ACCEPTED);
            applicationRepository.save(app);
            notifyResult(app, "ACCEPTED", listTypeByApp.get(app.getApplicationId()));
            count++;
        }

        // Reddedilenler: Dekan, Fakülte Kurulu reddini ÖİDB'ye iletince statü REJECTED olur;
        // ÖİDB yayınlayınca red kaydı oluşur (öğrenci bilgilendirilir). Statü REJECTED kalır.
        for (Application app : findByStatus(ApplicationStatus.REJECTED)) {
            if (publishedResultRepository.existsByApplication_ApplicationId(app.getApplicationId())) {
                continue;
            }
            publishedResultRepository.save(PublishedResult.builder()
                    .application(app)
                    .finalDecision("REJECTED")
                    .publishedBy(publisher)
                    .build());
            notifyResult(app, "REJECTED", null);
            count++;
        }

        return count;
    }

    /**
     * Yayınlanan sonucu öğrenciye in-app bildirir (TC-6.0 POST-2).
     * TC-6.3: Bildirim hatası yayın işlemini geri almaz — hata loglanır, yayın devam eder.
     */
    private void notifyResult(Application app, String decision, String listType) {
        if (app.getStudent() == null || app.getStudent().getUserId() == null) return;
        try {
            String message;
            if ("ACCEPTED".equals(decision)) {
                String liste = "WAITLIST".equals(listType) ? " (Yedek liste)"
                        : "PRIMARY".equals(listType) ? " (Asil liste)" : "";
                message = "Başvurunuz KABUL edildi" + liste + ". Sonuç ekranınızdan detayları görüntüleyebilirsiniz.";
            } else {
                message = "Başvurunuz REDDEDİLDİ. Sonuç ekranınızdan detayları görüntüleyebilirsiniz.";
            }
            notificationService.create(app.getStudent().getUserId(), "Başvuru Sonucu Yayınlandı", message);
        } catch (Exception e) {
            log.warn("Bildirim gönderilemedi — applicationId={}, karar={}, hata={}",
                    app.getApplicationId(), decision, e.getMessage());
        }
    }

    /**
     * Bölüm bazında asil/yedek atar: kabul edilen başvuruları (yeni {@code OIDB_FINAL_REVIEW} +
     * önceden {@code ACCEPTED}) bölüm-içi {@code EvaluationResult.ranking}'e göre sıralar; ilk
     * {@code Department.quota} → PRIMARY (asil), gerisi → WAITLIST (yedek). Quota null ise sınırsız
     * (hepsi asil). Bölümü olmayan başvuru atama dışı kalır (listType null).
     */
    private Map<Integer, String> computeListTypes() {
        List<Application> accepted = new ArrayList<>();
        accepted.addAll(findByStatus(ApplicationStatus.OIDB_FINAL_REVIEW));
        accepted.addAll(findByStatus(ApplicationStatus.ACCEPTED));

        Map<Integer, List<Application>> byDepartment = accepted.stream()
                .filter(a -> a.getDepartment() != null)
                .collect(Collectors.groupingBy(a -> a.getDepartment().getDepartmentId()));

        Map<Integer, String> listTypeByApp = new HashMap<>();
        for (List<Application> deptApps : byDepartment.values()) {
            deptApps.sort(Comparator.comparing(this::rankingOf,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            Integer quota = deptApps.get(0).getDepartment().getQuota();
            for (int i = 0; i < deptApps.size(); i++) {
                boolean asil = quota == null || i < quota;
                listTypeByApp.put(deptApps.get(i).getApplicationId(), asil ? "PRIMARY" : "WAITLIST");
            }
        }
        return listTypeByApp;
    }

    /** Başvurunun bölüm-içi sıralaması (yoksa null → sona). */
    private Integer rankingOf(Application app) {
        return evaluationResultRepository.findByApplication_ApplicationId(app.getApplicationId())
                .map(EvaluationResult::getRanking)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PublishedResultResponse> getResults() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT") || a.getAuthority().equals("STUDENT"));

        List<PublishedResult> results = isStudent
                ? publishedResultRepository.findByApplication_Student_Email(authentication.getName())
                : publishedResultRepository.findAll();

        return results.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private List<Application> findByStatus(ApplicationStatus status) {
        return new ArrayList<>(applicationRepository.findByStatus(status, Pageable.unpaged()).getContent());
    }

    private User resolveCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı: " + email));
    }

    private PublishedResultResponse toResponse(PublishedResult result) {
        Application app = result.getApplication();
        return PublishedResultResponse.builder()
                .applicationId(app.getApplicationId())
                .studentName(buildFullName(app.getStudent()))
                .finalDecision(result.getFinalDecision())
                .listType(result.getListType())
                .publishedAt(result.getPublishedAt())
                .build();
    }

    private String buildFullName(Student student) {
        if (student == null) {
            return null;
        }
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
}
