package edu.iztech.utms.g02.utms_app.bl.audit;

import edu.iztech.utms.g02.utms_app.api.audit.dto.AuditLogResponse;
import edu.iztech.utms.g02.utms_app.dal.audit.entity.AuditLog;
import edu.iztech.utms.g02.utms_app.dal.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Denetim kaydı yazımı. Kritik aksiyonlar (karar, iletim, skorlama, yayınlama) {@link #record}
 * ile {@code audit_log} tablosuna işlenir. Çağıranın transaction'ına katılır (aksiyon commit
 * olursa audit da commit olur).
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Bir aksiyonu denetim izine yazar.
     *
     * @param action        sabit aksiyon kodu (ör. {@code "FACULTY_BOARD_DECISION"})
     * @param applicationId ilgili başvuru (toplu aksiyonlarda null olabilir)
     * @param details       serbest metin detay (karar tipi, statü geçişi, sayı vb.)
     */
    public void record(String action, Integer applicationId, String details) {
        auditLogRepository.save(AuditLog.builder()
                .actor(currentActor())
                .action(action)
                .applicationId(applicationId)
                .details(details)
                .build());
    }

    /** Tüm denetim kayıtları, en yeni önce (ÖİDB inceleme — TC-6.5). */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> recent() {
        return auditLogRepository.findAllByOrderByTimestampDesc().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    /** Tek bir başvurunun denetim izi, eskiden yeniye. */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> forApplication(Integer applicationId) {
        return auditLogRepository.findByApplicationIdOrderByTimestampAsc(applicationId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .actor(a.getActor())
                .action(a.getAction())
                .applicationId(a.getApplicationId())
                .details(a.getDetails())
                .timestamp(a.getTimestamp())
                .build();
    }

    /** Oturum açan kullanıcının e-postası; güvenlik bağlamı yoksa "system". */
    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "system";
    }
}
