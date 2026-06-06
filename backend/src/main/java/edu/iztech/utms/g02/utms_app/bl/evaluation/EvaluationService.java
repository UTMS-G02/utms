package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.evaluation.dto.EvaluationResponse;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.YgkEvaluationRequest;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.CommitteeDecision;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.EvaluationResult;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.CommitteeDecisionRepository;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.EvaluationResultRepository;
import edu.iztech.utms.g02.utms_app.bl.audit.AuditService;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Staff;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Student;
import edu.iztech.utms.g02.utms_app.dal.user.repository.StaffRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * YGK aşaması: bekleyen başvuruları toplu skorlar ve sıralar.
 *
 * <ul>
 *   <li>Skor formülü {@link ScoreCalculator}'da sabittir, istekten okunmaz.</li>
 *   <li>Sıralama TEK seferde (toplu) yapılır, başvuru başına değil.</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EvaluationService {

    private final ApplicationRepository applicationRepository;
    private final EvaluationResultRepository evaluationResultRepository;
    private final CommitteeDecisionRepository committeeDecisionRepository;
    private final StaffRepository staffRepository; // Pair 3: oturum açan YGK üyesinin bölümünü çözmek için
    private final AuditService auditService;       // denetim izi (TC-8.0 POST-6)

    /**
     * Sıralama düzeni: yüksek compositeScore önce; eşit skorda tie-break uygulanır.
     *
     * <p>Tie-break (TestReport PRE-1): eşit skorda erken başvuran önce gelir.
     * Spec'in istediği alan {@code submissionDate}'dir (Pair 2 doldurunca otomatik devreye
     * girer). {@code submissionDate} şu an dolmadığından, her zaman dolu olan {@code createdAt}
     * anlamlı ve deterministik bir tie-break sağlar. Son olarak {@code applicationId} ile
     * tam belirlilik garanti edilir (artan id ≈ erken oluşturma).
     */
    private static final Comparator<EvaluationResult> RANKING_ORDER =
            Comparator.comparingDouble((EvaluationResult r) -> -r.getCompositeScore())
                    .thenComparing(r -> r.getApplication().getSubmissionDate(),
                            Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()))
                    .thenComparing(r -> r.getApplication().getCreatedAt(),
                            Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder()))
                    .thenComparing(r -> r.getApplication().getApplicationId());

    /**
     * Devir (handoff) statüsündeki tüm başvuruları skorlar, EvaluationResult üretir,
     * statülerini YGK_SCORED yapar ve tüm değerlendirmeleri yeniden sıralar.
     *
     * <p>Giriş statüsü {@code EVALUATION_QUEUE}'dir; Pair 2 kabul edilen başvuruları
     * OİDB son onayında bu statüye taşır (ApplicationService.processOidbPostYdyoReview).
     *
     * @return skorlanan başvuru sayısı
     */
    @Transactional
    public int scoreAllPendingApplications() {
        // #6: Her YGK YALNIZCA kendi bölümünü skorlar (18 bölüm YGK'sı).
        Integer departmentId = currentYgkDepartmentId();

        List<Application> pending = applicationRepository
                .findByStatusAndDepartment_DepartmentId(ApplicationStatus.EVALUATION_QUEUE, departmentId, Pageable.unpaged())
                .getContent();

        for (Application app : pending) {
            double score = ScoreCalculator.calculate(app.getGpa(), app.getSayYksScore());

            EvaluationResult result = evaluationResultRepository
                    .findByApplication_ApplicationId(app.getApplicationId())
                    .orElseGet(() -> EvaluationResult.builder().application(app).build());
            result.setCompositeScore(score);
            evaluationResultRepository.save(result);

            // compositeScore başvuruda da saklanır (Pair 2 alanı, read-only kullanım)
            app.setCompositeScore(score);
            app.setStatus(ApplicationStatus.YGK_SCORED);
            applicationRepository.save(app);
        }

        updateRankings(departmentId);
        auditService.record("YGK_SCORE_ALL", null,
                "departmentId=" + departmentId + ", count=" + pending.size());
        return pending.size();
    }

    /** Bölüm İÇİNDE skora ve tie-break'e göre sıralayıp ranking atar (1 = bölümün en yükseği). */
    private void updateRankings(Integer departmentId) {
        List<EvaluationResult> all = new ArrayList<>(
                evaluationResultRepository.findByApplication_Department_DepartmentId(departmentId));
        all.sort(RANKING_ORDER);
        int rank = 1;
        for (EvaluationResult result : all) {
            result.setRanking(rank++);
            evaluationResultRepository.save(result);
        }
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> getEvaluations() {
        // #6: YGK yalnızca kendi bölümünün değerlendirmelerini görür.
        return evaluationResultRepository
                .findByApplication_Department_DepartmentId(currentYgkDepartmentId()).stream()
                .sorted(RANKING_ORDER)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Oturum açan YGK üyesinin bölüm kimliği; atanmamışsa erişim reddedilir (bölüm-bazlı kapsam). */
    private Integer currentYgkDepartmentId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Staff ygk = staffRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("YGK personeli bulunamadı: " + email));
        if (ygk.getDepartmentId() == null) {
            throw new AccessDeniedException("YGK hesabına bölüm atanmamış.");
        }
        return ygk.getDepartmentId();
    }

    /**
     * YGK'nin tek bir başvuru için değerlendirmesini kaydeder ve Dekanlığa iletir (UC-8).
     *
     * <p>Skorlama (score-all) toplu yapılır; bu uç başvuru bazındadır: bölüm koşullarını ve
     * ayrı bir "Genel Değerlendirme Notu"nu kaydeder, YGK değerlendirmesini tamamlar.
     * Statü {@code YGK_SCORED} → {@code YGK_REVIEW_DONE} olur (kuyruktan çıkar); karar,
     * ekle-only {@code committee_decisions} tablosuna {@code decisionBy="YGK"} olarak işlenir.
     *
     * @return güncellenmiş değerlendirme satırı
     */
    @Transactional
    public EvaluationResponse submitEvaluation(Integer applicationId, YgkEvaluationRequest req) {
        if (req.getConditionsMet() == null) {
            throw new IllegalArgumentException("Karar (conditionsMet) zorunludur.");
        }
        if (req.getGeneralNote() == null || req.getGeneralNote().isBlank()) {
            throw new IllegalArgumentException("Genel değerlendirme notu zorunludur.");
        }

        Application app = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Başvuru bulunamadı. ID: " + applicationId));

        if (app.getStatus() != ApplicationStatus.YGK_SCORED) {
            throw new IllegalStateException(
                    "Başvuru YGK değerlendirmesine uygun aşamada değil. Güncel statü: " + app.getStatus());
        }

        EvaluationResult result = evaluationResultRepository
                .findByApplication_ApplicationId(applicationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Skor kaydı bulunamadı; önce toplu skorlama yapılmalıdır. ID: " + applicationId));

        result.setGeneralNote(req.getGeneralNote());
        evaluationResultRepository.save(result);

        ApplicationStatus previous = app.getStatus();
        app.setYgkApproved(req.getConditionsMet());
        app.setYgkReviewedDate(LocalDateTime.now());
        app.setStatus(ApplicationStatus.YGK_REVIEW_DONE);
        applicationRepository.save(app);

        // Ekle-only denetim izi: YGK değerlendirmesi (kim/ne zaman/sonuç + genel not).
        committeeDecisionRepository.save(CommitteeDecision.builder()
                .application(app)
                .decisionBy("YGK")
                .decision(req.getConditionsMet() ? "CONDITIONS_MET" : "CONDITIONS_NOT_MET")
                .notes(req.getGeneralNote())
                .build());

        log.info("YGK değerlendirmesi tamamlandı: applicationId={}, {} -> {}, conditionsMet={}, by={}",
                applicationId, previous, ApplicationStatus.YGK_REVIEW_DONE, req.getConditionsMet(), currentUser());

        auditService.record("YGK_EVALUATION_SUBMITTED", applicationId,
                "conditionsMet=" + req.getConditionsMet());

        return toResponse(result);
    }

    /** Denetim logu için aktif kullanıcının e-postası; güvenlik bağlamı yoksa "anonymous". */
    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";
    }

    private EvaluationResponse toResponse(EvaluationResult result) {
        Application app = result.getApplication();
        Integer quota = app.getDepartment() != null ? app.getDepartment().getQuota() : null;
        return EvaluationResponse.builder()
                .applicationId(app.getApplicationId())
                .studentName(buildFullName(app.getStudent()))
                .gpa(app.getGpa())
                .yksScore(app.getSayYksScore())
                .compositeScore(result.getCompositeScore())
                .ranking(result.getRanking())
                .status(app.getStatus())
                .calculatedAt(result.getCalculatedAt())
                .conditionsMet(app.getYgkApproved())
                .generalNote(result.getGeneralNote())
                .provisionalListType(provisionalListType(result.getRanking(), quota))
                .build();
    }

    /** Provizyonel asil/yedek: ranking ≤ quota → PRIMARY; quota null → sınırsız (PRIMARY); ranking null → null. */
    static String provisionalListType(Integer ranking, Integer quota) {
        if (ranking == null) return null;
        return (quota == null || ranking <= quota) ? "PRIMARY" : "WAITLIST";
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
