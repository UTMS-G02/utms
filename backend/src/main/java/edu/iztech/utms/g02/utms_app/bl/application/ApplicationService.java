package edu.iztech.utms.g02.utms_app.bl.application;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationCreateRequest;
import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.api.application.dto.OidbReviewRequest;
import edu.iztech.utms.g02.utms_app.api.application.dto.YdyoReviewRequest;

import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;

import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import edu.iztech.utms.g02.utms_app.dal.application.repository.DocumentRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public ApplicationResponse create(Long userId, ApplicationCreateRequest req) {

        // İş kuralı: aynı öğrenci, aynı bölüm, aynı dönem için ikinci başvuru yapamaz
        boolean alreadyApplied = applicationRepository.existsByStudentIdAndTargetDeptAndAcademicYear(
                req.getStudentId(),
                req.getTargetDept(),
                req.getAcademicYear()
        );

        if (alreadyApplied) {
            throw new IllegalArgumentException("Bir öğrenci aynı bölüme, aynı akademik dönemde birden fazla başvuru yapamaz.");
        }

        if (!Boolean.TRUE.equals(req.getKvkkAccepted())) {
            throw new IllegalArgumentException("KVKK onayı zorunludur.");
        }

        Application app = Application.builder()
                .studentId(req.getStudentId())
                .status(ApplicationStatus.DRAFT)
                .academicYear(req.getAcademicYear())
                .targetDept(req.getTargetDept())
                .targetFaculty(req.getTargetFaculty())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        app = applicationRepository.save(app);
        return toResponse(app);
    }

    @Transactional
    public ApplicationResponse submit(Long applicationId, Long userId) {

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new IllegalStateException("Application is not in DRAFT state");
        }

        app.setStatus(ApplicationStatus.SUBMITTED);
        app.setUpdatedAt(LocalDateTime.now());
        app = applicationRepository.save(app);
        return toResponse(app);
    }

    public List<ApplicationResponse> getAllApplications() {
        return applicationRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ApplicationResponse getApplicationById(Long id) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        return toResponse(app);
    }

    @Transactional
    public ApplicationResponse processOidbReview(Long id, OidbReviewRequest req) {

        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        app.setStatus(req.isApproved() ? ApplicationStatus.YDYO_REVIEW : ApplicationStatus.OIDB_REJECTED);
        app.setOidbApproved(req.isApproved());
        app.setOidbNotes(req.getNotes());
        app.setOidbReviewedBy(req.getReviewerId());
        app.setOidbReviewedDate(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());

        app = applicationRepository.save(app);
        return toResponse(app);
    }

    @Transactional
    public ApplicationResponse processYdyoReview(Long id, YdyoReviewRequest req) {

        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        app.setStatus(req.isApproved() ? ApplicationStatus.EVALUATION_QUEUE : ApplicationStatus.YDYO_REJECTED);
        app.setYdyoApproved(req.isApproved());
        app.setYdyoNotes(req.getNotes());
        app.setYdyoReviewedBy(req.getReviewerId());
        app.setYdyoReviewedDate(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());

        app = applicationRepository.save(app);
        return toResponse(app);
    }

    public void uploadDocument(Long applicationId, MultipartFile file) {
        // TODO: Belge yükleme mantığı DocumentService'e taşınacak
        throw new UnsupportedOperationException("Belge yükleme henüz implemente edilmedi.");
    }

    private ApplicationResponse toResponse(Application app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .studentId(app.getStudentId())
                .status(app.getStatus())
                .academicYear(app.getAcademicYear())
                .targetDept(app.getTargetDept())
                .targetFaculty(app.getTargetFaculty())
                .oidbApproved(app.getOidbApproved())
                .oidbNotes(app.getOidbNotes())
                .oidbReviewedBy(app.getOidbReviewedBy())
                .oidbReviewedDate(app.getOidbReviewedDate())
                .ydyoApproved(app.getYdyoApproved())
                .ydyoNotes(app.getYdyoNotes())
                .ydyoReviewedBy(app.getYdyoReviewedBy())
                .ydyoReviewedDate(app.getYdyoReviewedDate())
                .build();
    }
}