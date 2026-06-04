package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.evaluation.dto.CourseEquivalencyRow;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.IntibakTableRequest;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.IntibakTableResponse;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.CourseEquivalency;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.CourseEquivalencyRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class CourseEquivalencyService {

    private final ApplicationRepository applicationRepository;
    private final CourseEquivalencyRepository courseEquivalencyRepository;

    @Transactional(readOnly = true)
    public IntibakTableResponse getTable(Integer applicationId) {
        Application app = findApp(applicationId);
        List<CourseEquivalency> rows =
                courseEquivalencyRepository.findByApplication_ApplicationIdOrderByRowOrderAsc(applicationId);

        return IntibakTableResponse.builder()
                .applicationId(applicationId)
                .conditionsMet(app.getYgkApproved())
                .shortcomingNote(app.getYgkNotes())
                .rows(rows.stream().map(this::toRow).collect(Collectors.toList()))
                .build();
    }

    /**
     * Tablonun tamamını kaydeder (replace-all). Mevcut satırlar silinip yenileri eklenir.
     * Koşullar karşılanmıyorsa shortcomingNote zorunludur ve rows görmezden gelinir.
     */
    @Transactional
    public IntibakTableResponse saveTable(Integer applicationId, IntibakTableRequest req) {
        if (req.getConditionsMet() == null) {
            throw new IllegalArgumentException("conditionsMet zorunludur.");
        }
        if (Boolean.FALSE.equals(req.getConditionsMet())
                && (req.getShortcomingNote() == null || req.getShortcomingNote().isBlank())) {
            throw new IllegalArgumentException("Koşullar karşılanmıyorsa shortcomingNote zorunludur.");
        }

        Application app = findApp(applicationId);

        // Koşullar karşılanıyorsa tablo eksiksiz olmalı: durumu/satırları DEĞİŞTİRMEDEN
        // önce doğrula ki hatalı istek mevcut tabloyu silmesin (EX-2).
        List<CourseEquivalencyRow> rows = null;
        if (Boolean.TRUE.equals(req.getConditionsMet())) {
            rows = req.getRows();
            validateRows(rows);
        }

        app.setYgkApproved(req.getConditionsMet());
        app.setYgkNotes(req.getShortcomingNote());
        applicationRepository.save(app);

        // Eski satırları sil, yenileri ekle
        courseEquivalencyRepository.deleteByApplication_ApplicationId(applicationId);

        List<CourseEquivalency> newRows = Collections.emptyList();
        if (rows != null) {
            final List<CourseEquivalencyRow> validRows = rows;
            newRows = IntStream.range(0, validRows.size())
                    .mapToObj(i -> toEntity(validRows.get(i), app, i))
                    .collect(Collectors.toList());
            courseEquivalencyRepository.saveAll(newRows);
        }

        return IntibakTableResponse.builder()
                .applicationId(applicationId)
                .conditionsMet(req.getConditionsMet())
                .shortcomingNote(req.getShortcomingNote())
                .rows(newRows.stream().map(this::toRow).collect(Collectors.toList()))
                .build();
    }

    /**
     * Koşullar karşılanıyorken intibak tablosunun eksiksizliğini doğrular (EX-2).
     * En az bir ders satırı gerekir; her satırda ders kodu/adı dolu, krediler pozitif
     * ve denklik durumu seçili olmalıdır. Hata mesajları kullanıcı dostudur ve
     * 1 tabanlı satır numarasını içerir.
     */
    private void validateRows(List<CourseEquivalencyRow> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "İntibak tablosu eksik: koşullar karşılanıyorsa en az bir ders satırı girilmelidir.");
        }
        for (int i = 0; i < rows.size(); i++) {
            CourseEquivalencyRow row = rows.get(i);
            int rowNo = i + 1;
            requireText(row.getSourceCode(), rowNo, "kaynak ders kodu");
            requireText(row.getSourceName(), rowNo, "kaynak ders adı");
            requirePositive(row.getSourceCredit(), rowNo, "kaynak kredi");
            requireText(row.getTargetCode(), rowNo, "hedef ders kodu");
            requireText(row.getTargetName(), rowNo, "hedef ders adı");
            requirePositive(row.getTargetCredit(), rowNo, "hedef kredi");
            if (row.getEquivalencyStatus() == null) {
                throw new IllegalArgumentException(
                        "İntibak tablosu eksik: " + rowNo + ". satırda denklik durumu seçilmelidir.");
            }
        }
    }

    private void requireText(String value, int rowNo, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "İntibak tablosu eksik: " + rowNo + ". satırda " + field + " zorunludur.");
        }
    }

    private void requirePositive(Integer value, int rowNo, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(
                    "İntibak tablosu eksik: " + rowNo + ". satırda " + field + " pozitif olmalıdır.");
        }
    }

    private CourseEquivalency toEntity(CourseEquivalencyRow row, Application app, int order) {
        return CourseEquivalency.builder()
                .application(app)
                .sourceCode(HtmlUtils.htmlEscape(row.getSourceCode()))
                .sourceName(HtmlUtils.htmlEscape(row.getSourceName()))
                .sourceCredit(row.getSourceCredit())
                .targetCode(HtmlUtils.htmlEscape(row.getTargetCode()))
                .targetName(HtmlUtils.htmlEscape(row.getTargetName()))
                .targetCredit(row.getTargetCredit())
                .equivalencyStatus(row.getEquivalencyStatus())
                .rowOrder(order)
                .build();
    }

    private CourseEquivalencyRow toRow(CourseEquivalency entity) {
        return CourseEquivalencyRow.builder()
                .sourceCode(entity.getSourceCode())
                .sourceName(entity.getSourceName())
                .sourceCredit(entity.getSourceCredit())
                .targetCode(entity.getTargetCode())
                .targetName(entity.getTargetName())
                .targetCredit(entity.getTargetCredit())
                .equivalencyStatus(entity.getEquivalencyStatus())
                .build();
    }

    private Application findApp(Integer id) {
        return applicationRepository.findByApplicationId(id)
                .orElseThrow(() -> new EntityNotFoundException("Başvuru bulunamadı. ID: " + id));
    }
}
