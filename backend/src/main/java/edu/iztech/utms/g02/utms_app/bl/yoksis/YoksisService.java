package edu.iztech.utms.g02.utms_app.bl.yoksis;

import edu.iztech.utms.g02.utms_app.api.yoksis.dto.YoksisMeResponse;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Student;
import edu.iztech.utms.g02.utms_app.dal.user.repository.StudentRepository;
import edu.iztech.utms.g02.utms_app.integration.edevlet.EgovDocumentService;
import edu.iztech.utms.g02.utms_app.integration.yoksis.YoksisIntegrationService;
import edu.iztech.utms.g02.utms_app.integration.yoksis.GpaScaleConverter;
import edu.iztech.utms.g02.utms_app.integration.yoksis.dto.YoksisStudentResponse;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class YoksisService {

    private final StudentRepository studentRepository;
    private final YoksisIntegrationService yoksisIntegrationService;
    private final EgovDocumentService egovDocumentService;

    // e-Devlet/ÖSYM önizleme belgesi: dosya adı + PDF içeriği.
    public record DocumentPreview(String fileName, byte[] content) {}

    // Resolves the authenticated student and returns their YÖKSİS academic data.
    // Uses the SAME source as ApplicationService.create, so the form preview and
    // the persisted application always agree.
    public YoksisMeResponse getForCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Öğrenci bulunamadı."));

        YoksisStudentResponse data = yoksisIntegrationService.fetchAcademicDataByTckn(student.getTckn());
        Integer currentClass = data.semester() == null ? null : (data.semester() + 1) / 2;

        // GPA'yı forma 4'lük sistemde yansıt: YÖKSİS 100'lük dönmüş olsa bile çevrilir.
        // Böylece formda görünen değer ile create() sırasında kaydedilen/doğrulanan değer
        // aynı (4'lük) ölçektedir.
        double normalizedGpa = GpaScaleConverter.toFourScale(data.gpa(), data.gpaScale());

        return new YoksisMeResponse(
                data.currentUniversity(),
                data.currentFaculty(),
                data.currentDepartment(),
                data.semester(),
                currentClass,
                normalizedGpa,
                data.yksScore(),
                data.yksRank()
        );
    }

    // "Yeni Başvuru" formunda, başvuru henüz oluşturulmadan e-Devlet/ÖSYM belgesini
    // önizlemek için: o anki öğrencinin YÖKSİS verisinden mock PDF'i bellekte üretir.
    public DocumentPreview getDocumentPreview(String type) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Öğrenci bulunamadı."));

        YoksisStudentResponse data = yoksisIntegrationService.fetchAcademicDataByTckn(student.getTckn());
        byte[] content = egovDocumentService.buildPreview(student, data, type);
        return new DocumentPreview(egovDocumentService.previewFileName(type), content);
    }
}
