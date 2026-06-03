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
        app.setYgkApproved(req.getConditionsMet());
        app.setYgkNotes(req.getShortcomingNote());
        applicationRepository.save(app);

        // Eski satırları sil, yenileri ekle
        courseEquivalencyRepository.deleteByApplication_ApplicationId(applicationId);

        List<CourseEquivalency> newRows = Collections.emptyList();
        if (Boolean.TRUE.equals(req.getConditionsMet()) && req.getRows() != null) {
            newRows = IntStream.range(0, req.getRows().size())
                    .mapToObj(i -> toEntity(req.getRows().get(i), app, i))
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
