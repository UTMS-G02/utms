package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.dal.user.entity.Staff;
import edu.iztech.utms.g02.utms_app.dal.user.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Oturum açan Dekanlık personelinin fakülte kimliğini çözer.
 *
 * <p>Hem fakülte-kapsamlı listeleme ({@link DeanQueueService}) hem de iletim
 * ({@link DeanForwardService}) tek bir kaynaktan dekanın {@code facultyId}'sini alır.
 * Personel bulunamaz veya hesaba fakülte atanmamışsa erişim reddedilir.
 */
@Component
@RequiredArgsConstructor
public class DeanIdentity {

    private final StaffRepository staffRepository;

    public Integer currentFacultyId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Staff dean = staffRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Dekanlık personeli bulunamadı: " + email));
        if (dean.getFacultyId() == null) {
            throw new AccessDeniedException("Dekan hesabına fakülte atanmamış.");
        }
        return dean.getFacultyId();
    }
}
