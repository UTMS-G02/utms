package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.evaluation.dto.PublishedResultResponse;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.PublishedResult;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.PublishedResultRepository;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Student;
import edu.iztech.utms.g02.utms_app.dal.user.entity.User;
import edu.iztech.utms.g02.utms_app.dal.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ÖİDB sonuç yayınlama ve sonuç görüntüleme.
 *
 * <p>Sonuçlar yayınlanana (published_results satırı oluşana) kadar öğrenciye görünmez.
 * Yayınlama nihai aşamadaki başvuruları kapsar:
 * RESULT_PUBLISHED → ACCEPTED; reddedilen terminal statüler → REJECTED kaydı.
 */
@Service
@RequiredArgsConstructor
public class ResultService {

    private static final Set<ApplicationStatus> REJECTED_TERMINAL = EnumSet.of(
            ApplicationStatus.DEAN_REJECTED,
            ApplicationStatus.FACULTY_BOARD_REJECTED,
            ApplicationStatus.REJECTED);

    private final ApplicationRepository applicationRepository;
    private final PublishedResultRepository publishedResultRepository;
    private final UserRepository userRepository;

    /**
     * Nihai aşamadaki başvuruları yayınlar. Halihazırda yayınlanmış başvurular atlanır
     * (idempotent). Kabul edilenlerin statüsü ACCEPTED yapılır.
     *
     * @return yayınlanan (yeni) sonuç sayısı
     */
    @Transactional
    public int publishResults() {
        User publisher = resolveCurrentUser();
        int count = 0;

        // Kabul edilenler: final dekanlık onayı sonrası RESULT_PUBLISHED → ACCEPTED
        for (Application app : findByStatus(ApplicationStatus.RESULT_PUBLISHED)) {
            if (publishedResultRepository.existsByApplication_ApplicationId(app.getApplicationId())) {
                continue;
            }
            publishedResultRepository.save(PublishedResult.builder()
                    .application(app)
                    .finalDecision("ACCEPTED")
                    .publishedBy(publisher)
                    .build());
            app.setStatus(ApplicationStatus.ACCEPTED);
            applicationRepository.save(app);
            count++;
        }

        // Reddedilenler: terminal red statülerini de öğrenciye görünür kıl
        for (ApplicationStatus status : REJECTED_TERMINAL) {
            for (Application app : findByStatus(status)) {
                if (publishedResultRepository.existsByApplication_ApplicationId(app.getApplicationId())) {
                    continue;
                }
                publishedResultRepository.save(PublishedResult.builder()
                        .application(app)
                        .finalDecision("REJECTED")
                        .publishedBy(publisher)
                        .build());
                count++;
            }
        }

        return count;
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
