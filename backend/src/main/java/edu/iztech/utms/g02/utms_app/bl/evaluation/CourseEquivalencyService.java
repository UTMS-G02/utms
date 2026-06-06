package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.evaluation.dto.CourseEquivalencyRow;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.IntibakTableRequest;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.IntibakTableResponse;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.CourseEquivalency;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.EquivalencyStatus;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.CourseEquivalencyRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class CourseEquivalencyService {

    private static final Set<String> VALID_GRADES =
            Set.of("AA", "BA", "BB", "CB", "CC", "DC", "DD", "FF");

    private final ApplicationRepository applicationRepository;
    private final CourseEquivalencyRepository courseEquivalencyRepository;

    // ============================================================
    // OKUMA
    // ============================================================

    @Transactional(readOnly = true)
    public IntibakTableResponse getTable(Integer applicationId) {
        Application app = findApp(applicationId);
        List<CourseEquivalency> rows =
                courseEquivalencyRepository.findByApplication_ApplicationIdOrderByRowOrderAsc(applicationId);

        return buildResponse(app, rows);
    }

    // ============================================================
    // TOPLU KAYIT (replace-all)
    // ============================================================

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

        List<CourseEquivalencyRow> rows = null;
        if (Boolean.TRUE.equals(req.getConditionsMet())) {
            rows = req.getRows();
            validateRows(rows);
        }

        app.setYgkApproved(req.getConditionsMet());
        app.setYgkNotes(req.getShortcomingNote());
        applicationRepository.save(app);

        courseEquivalencyRepository.deleteByApplication_ApplicationId(applicationId);

        List<CourseEquivalency> saved = Collections.emptyList();
        if (rows != null) {
            final List<CourseEquivalencyRow> validRows = rows;
            saved = IntStream.range(0, validRows.size())
                    .mapToObj(i -> toEntity(validRows.get(i), app, i))
                    .collect(Collectors.toList());
            courseEquivalencyRepository.saveAll(saved);
        }

        return buildResponse(app, saved);
    }

    // ============================================================
    // TEK SATIR İŞLEMLERİ
    // ============================================================

    @Transactional
    public IntibakTableResponse addRow(Integer applicationId, CourseEquivalencyRow row) {
        Application app = findApp(applicationId);
        validateRow(row, 1);

        int nextOrder = courseEquivalencyRepository.countByApplication_ApplicationId(applicationId);
        courseEquivalencyRepository.save(toEntity(row, app, nextOrder));

        return buildResponse(app,
                courseEquivalencyRepository.findByApplication_ApplicationIdOrderByRowOrderAsc(applicationId));
    }

    @Transactional
    public IntibakTableResponse editRow(Integer applicationId, Integer rowId, CourseEquivalencyRow row) {
        Application app = findApp(applicationId);
        CourseEquivalency existing = findRowForApp(rowId, applicationId);
        validateRow(row, 1);

        existing.setSourceCode(HtmlUtils.htmlEscape(row.getSourceCode()));
        existing.setSourceName(HtmlUtils.htmlEscape(row.getSourceName()));
        existing.setSourceCredit(row.getSourceCredit());
        existing.setSourceGrade(row.getSourceGrade());
        existing.setSource2Code(row.getSource2Code() != null ? HtmlUtils.htmlEscape(row.getSource2Code()) : null);
        existing.setSource2Name(row.getSource2Name() != null ? HtmlUtils.htmlEscape(row.getSource2Name()) : null);
        existing.setSource2Credit(row.getSource2Credit());
        existing.setSource2Grade(row.getSource2Grade());
        existing.setTargetCode(HtmlUtils.htmlEscape(row.getTargetCode()));
        existing.setTargetName(HtmlUtils.htmlEscape(row.getTargetName()));
        existing.setTargetCredit(row.getTargetCredit());
        existing.setTargetGrade(row.getTargetGrade());
        existing.setEquivalencyStatus(row.getEquivalencyStatus());
        courseEquivalencyRepository.save(existing);

        return buildResponse(app,
                courseEquivalencyRepository.findByApplication_ApplicationIdOrderByRowOrderAsc(applicationId));
    }

    @Transactional
    public IntibakTableResponse deleteRow(Integer applicationId, Integer rowId) {
        Application app = findApp(applicationId);
        CourseEquivalency existing = findRowForApp(rowId, applicationId);
        courseEquivalencyRepository.delete(existing);

        return buildResponse(app,
                courseEquivalencyRepository.findByApplication_ApplicationIdOrderByRowOrderAsc(applicationId));
    }

    // ============================================================
    // %80 DENKLİK ORANI
    // ============================================================

    /**
     * Kredi ağırlıklı denklik oranı (0.0–1.0).
     * conditionsMet=false veya tablo boşsa null döner.
     */
    public Double calculateEquivalencyRatio(Integer applicationId) {
        Application app = findApp(applicationId);
        if (!Boolean.TRUE.equals(app.getYgkApproved())) return null;

        List<CourseEquivalency> rows =
                courseEquivalencyRepository.findByApplication_ApplicationIdOrderByRowOrderAsc(applicationId);
        if (rows.isEmpty()) return null;

        int total = rows.stream().mapToInt(this::effectiveCredits).sum();
        if (total == 0) return null;

        int equivalent = rows.stream()
                .filter(r -> r.getEquivalencyStatus() != EquivalencyStatus.DENK_DEGIL)
                .mapToInt(this::effectiveCredits)
                .sum();

        return (double) equivalent / total;
    }

    // ============================================================
    // YARDIMCI — VALİDASYON
    // ============================================================

    private void validateRows(List<CourseEquivalencyRow> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "İntibak tablosu eksik: koşullar karşılanıyorsa en az bir ders satırı girilmelidir.");
        }
        for (int i = 0; i < rows.size(); i++) {
            validateRow(rows.get(i), i + 1);
        }
    }

    private void validateRow(CourseEquivalencyRow row, int rowNo) {
        requireText(row.getSourceCode(), rowNo, "kaynak ders kodu");
        requireText(row.getSourceName(), rowNo, "kaynak ders adı");
        requirePositive(row.getSourceCredit(), rowNo, "kaynak kredi");
        requireGrade(row.getSourceGrade(), rowNo, "kaynak not");

        validateSource2(row, rowNo);

        if (row.getEquivalencyStatus() == null) {
            throw new IllegalArgumentException(
                    "İntibak tablosu eksik: " + rowNo + ". satırda denklik durumu seçilmelidir.");
        }

        // Hedef ders alanları yalnızca TAM_DENKLIK ve KISMI_DENKLIK satırlarında zorunludur.
        // DENK_DEGIL satırlarında hedef bilgisi olmayabilir (TC-9.0).
        if (row.getEquivalencyStatus() != EquivalencyStatus.DENK_DEGIL) {
            requireText(row.getTargetCode(), rowNo, "hedef ders kodu");
            requireText(row.getTargetName(), rowNo, "hedef ders adı");
            requirePositive(row.getTargetCredit(), rowNo, "hedef kredi");
            requireGrade(row.getTargetGrade(), rowNo, "hedef not");
        }
    }

    private void validateSource2(CourseEquivalencyRow row, int rowNo) {
        boolean hasCode   = row.getSource2Code()   != null && !row.getSource2Code().isBlank();
        boolean hasName   = row.getSource2Name()   != null && !row.getSource2Name().isBlank();
        boolean hasCredit = row.getSource2Credit() != null;
        boolean hasGrade  = row.getSource2Grade()  != null && !row.getSource2Grade().isBlank();

        long filledCount = List.of(hasCode, hasName, hasCredit, hasGrade)
                .stream().filter(b -> b).count();

        if (filledCount == 0) return; // 1→1 satır, geçerli

        if (filledCount < 4) {
            throw new IllegalArgumentException(
                    rowNo + ". satır: 2. kaynak ders alanları ya tamamı dolu ya da tamamı boş olmalıdır " +
                    "(sourceCode, sourceName, sourceCredit, sourceGrade).");
        }

        requirePositive(row.getSource2Credit(), rowNo, "2. kaynak kredi");
        requireGrade(row.getSource2Grade(), rowNo, "2. kaynak not");
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

    private void requireGrade(String value, int rowNo, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "İntibak tablosu eksik: " + rowNo + ". satırda " + field + " zorunludur.");
        }
        if (!VALID_GRADES.contains(value.toUpperCase())) {
            throw new IllegalArgumentException(
                    rowNo + ". satırda " + field + " geçersiz: \"" + value +
                    "\". Geçerli notlar: AA, BA, BB, CB, CC, DC, DD, FF.");
        }
    }

    // ============================================================
    // YARDIMCI — DÖNÜŞÜM
    // ============================================================

    private IntibakTableResponse buildResponse(Application app, List<CourseEquivalency> rows) {
        List<CourseEquivalencyRow> rowDtos = rows.stream().map(this::toRow).collect(Collectors.toList());
        Double ratio = Boolean.TRUE.equals(app.getYgkApproved()) ? computeRatio(rows) : null;
        return IntibakTableResponse.builder()
                .applicationId(app.getApplicationId())
                .conditionsMet(app.getYgkApproved())
                .shortcomingNote(app.getYgkNotes())
                .rows(rowDtos)
                .equivalencyRatio(ratio)
                .build();
    }

    private Double computeRatio(List<CourseEquivalency> rows) {
        if (rows.isEmpty()) return null;
        int total = rows.stream().mapToInt(this::effectiveCredits).sum();
        if (total == 0) return null;
        int equivalent = rows.stream()
                .filter(r -> r.getEquivalencyStatus() != EquivalencyStatus.DENK_DEGIL)
                .mapToInt(this::effectiveCredits)
                .sum();
        return (double) equivalent / total;
    }

    private int effectiveCredits(CourseEquivalency row) {
        int credits = row.getSourceCredit() != null ? row.getSourceCredit() : 0;
        if (row.getSource2Credit() != null) credits += row.getSource2Credit();
        return credits;
    }

    private CourseEquivalency toEntity(CourseEquivalencyRow row, Application app, int order) {
        return CourseEquivalency.builder()
                .application(app)
                .sourceCode(HtmlUtils.htmlEscape(row.getSourceCode()))
                .sourceName(HtmlUtils.htmlEscape(row.getSourceName()))
                .sourceCredit(row.getSourceCredit())
                .sourceGrade(row.getSourceGrade())
                .source2Code(row.getSource2Code() != null ? HtmlUtils.htmlEscape(row.getSource2Code()) : null)
                .source2Name(row.getSource2Name() != null ? HtmlUtils.htmlEscape(row.getSource2Name()) : null)
                .source2Credit(row.getSource2Credit())
                .source2Grade(row.getSource2Grade())
                .targetCode(row.getTargetCode() != null ? HtmlUtils.htmlEscape(row.getTargetCode()) : null)
                .targetName(row.getTargetName() != null ? HtmlUtils.htmlEscape(row.getTargetName()) : null)
                .targetCredit(row.getTargetCredit())
                .targetGrade(row.getTargetGrade())
                .equivalencyStatus(row.getEquivalencyStatus())
                .rowOrder(order)
                .build();
    }

    private CourseEquivalencyRow toRow(CourseEquivalency entity) {
        return CourseEquivalencyRow.builder()
                .id(entity.getId())
                .sourceCode(entity.getSourceCode())
                .sourceName(entity.getSourceName())
                .sourceCredit(entity.getSourceCredit())
                .sourceGrade(entity.getSourceGrade())
                .source2Code(entity.getSource2Code())
                .source2Name(entity.getSource2Name())
                .source2Credit(entity.getSource2Credit())
                .source2Grade(entity.getSource2Grade())
                .targetCode(entity.getTargetCode())
                .targetName(entity.getTargetName())
                .targetCredit(entity.getTargetCredit())
                .targetGrade(entity.getTargetGrade())
                .equivalencyStatus(entity.getEquivalencyStatus())
                .build();
    }

    private CourseEquivalency findRowForApp(Integer rowId, Integer applicationId) {
        return courseEquivalencyRepository.findByIdAndApplication_ApplicationId(rowId, applicationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Satır bulunamadı veya bu başvuruya ait değil. rowId: " + rowId));
    }

    private Application findApp(Integer id) {
        return applicationRepository.findByApplicationId(id)
                .orElseThrow(() -> new EntityNotFoundException("Başvuru bulunamadı. ID: " + id));
    }
}
