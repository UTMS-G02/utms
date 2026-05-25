package edu.iztech.utms.g02.utms_app.api.application.controller;

import edu.iztech.utms.g02.utms_app.bl.application.DocumentService;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping(value = "/applications/{applicationId}", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @PathVariable Integer applicationId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentType", required = false) String documentType) {

        Document document = documentService.uploadDocument(applicationId, documentType, file);
        return ResponseEntity.ok(toResponse(document));
    }

    @PreAuthorize("hasAnyRole('STUDENT', 'OIDB', 'YDYO', 'FACULTY', 'DEAN')")
    @GetMapping("/{documentId}")
    public ResponseEntity<Map<String, Object>> getDocumentById(@PathVariable Integer documentId) {
        Document document = documentService.getDocumentById(documentId);
        return ResponseEntity.ok(toResponse(document));
    }

    @PreAuthorize("hasAnyRole('STUDENT', 'OIDB', 'YDYO', 'FACULTY', 'DEAN')")
    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<List<Map<String, Object>>> getDocumentsByApplicationId(
            @PathVariable Integer applicationId) {

        List<Map<String, Object>> documents = documentService.getDocumentsByApplicationId(applicationId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(documents);
    }

    @PreAuthorize("hasAnyRole('OIDB', 'YDYO', 'FACULTY', 'DEAN')")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getDocumentsByType(
            @RequestParam("documentType") String documentType) {

        List<Map<String, Object>> documents = documentService.getDocumentsByType(documentType)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(documents);
    }

    @PreAuthorize("hasRole('YDYO')")
    @PatchMapping("/{documentId}/ydyo-approval")
    public ResponseEntity<Map<String, Object>> approveByYdyo(
            @PathVariable Integer documentId,
            @RequestParam boolean approved) {

        Document document = documentService.approveByYdyo(documentId, approved);
        return ResponseEntity.ok(toResponse(document));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @DeleteMapping("/{documentId}")
    public ResponseEntity<String> deactivateDocument(@PathVariable Integer documentId) {
        documentService.deactivateDocument(documentId);
        return ResponseEntity.ok("Belge pasife alindi.");
    }

    private Map<String, Object> toResponse(Document document) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("documentId", document.getDocumentId());
        response.put("applicationId", document.getApplication().getApplicationId());
        response.put("documentType", document.getDocumentType());
        response.put("fileName", document.getFileName());
        response.put("filePath", document.getFilePath());
        response.put("ydyoApproved", document.isYdyoApproved());
        response.put("documentUploadDate", document.getDocumentUploadDate());
        response.put("active", document.isActive());
        return response;
    }
}
