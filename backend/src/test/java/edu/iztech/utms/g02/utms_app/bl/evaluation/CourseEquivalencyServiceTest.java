package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.evaluation.dto.CourseEquivalencyRow;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.IntibakTableRequest;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.IntibakTableResponse;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.CourseEquivalency;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.EquivalencyStatus;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.CourseEquivalencyRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseEquivalencyServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private CourseEquivalencyRepository courseEquivalencyRepository;

    @InjectMocks private CourseEquivalencyService courseEquivalencyService;

    // ==========================================
    // GET TABLE
    // ==========================================

    @Test
    void getTable_returnsRowsInOrder() {
        Application app = buildApp(1);
        app.setYgkApproved(true);
        app.setYgkNotes(null);

        CourseEquivalency row1 = buildEntity(app, "MAT101", "Calculus", 3, "MAT101", "Matematik", 3, EquivalencyStatus.TAM_DENKLIK, 0);
        CourseEquivalency row2 = buildEntity(app, "FIZ101", "Physics",  4, "FIZ101", "Fizik",     4, EquivalencyStatus.KISMI_DENKLIK, 1);

        when(applicationRepository.findByApplicationId(1)).thenReturn(Optional.of(app));
        when(courseEquivalencyRepository.findByApplication_ApplicationIdOrderByRowOrderAsc(1))
                .thenReturn(List.of(row1, row2));

        IntibakTableResponse response = courseEquivalencyService.getTable(1);

        assertThat(response.getApplicationId()).isEqualTo(1);
        assertThat(response.getConditionsMet()).isTrue();
        assertThat(response.getRows()).hasSize(2);
        assertThat(response.getRows().get(0).getSourceCode()).isEqualTo("MAT101");
        assertThat(response.getRows().get(1).getSourceCode()).isEqualTo("FIZ101");
    }

    @Test
    void getTable_conditionsNotMet_returnsShortcomingNoteAndEmptyRows() {
        Application app = buildApp(2);
        app.setYgkApproved(false);
        app.setYgkNotes("Fizik dersi alınmamış.");

        when(applicationRepository.findByApplicationId(2)).thenReturn(Optional.of(app));
        when(courseEquivalencyRepository.findByApplication_ApplicationIdOrderByRowOrderAsc(2))
                .thenReturn(List.of());

        IntibakTableResponse response = courseEquivalencyService.getTable(2);

        assertThat(response.getConditionsMet()).isFalse();
        assertThat(response.getShortcomingNote()).isEqualTo("Fizik dersi alınmamış.");
        assertThat(response.getRows()).isEmpty();
    }

    @Test
    void getTable_applicationNotFound_throwsEntityNotFoundException() {
        when(applicationRepository.findByApplicationId(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseEquivalencyService.getTable(99))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==========================================
    // SAVE TABLE — koşullar karşılanıyor
    // ==========================================

    @Test
    void saveTable_conditionsMet_persistsRowsAndReturnsResponse() {
        Application app = buildApp(3);
        IntibakTableRequest req = new IntibakTableRequest(true, null, List.of(
                buildRow("MAT101", "Calculus", 3, "MAT101", "Matematik", 3, EquivalencyStatus.TAM_DENKLIK),
                buildRow("FIZ101", "Physics",  4, "FIZ101", "Fizik",     4, EquivalencyStatus.KISMI_DENKLIK)
        ));

        when(applicationRepository.findByApplicationId(3)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any())).thenReturn(app);
        when(courseEquivalencyRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        IntibakTableResponse response = courseEquivalencyService.saveTable(3, req);

        assertThat(response.getConditionsMet()).isTrue();
        assertThat(response.getRows()).hasSize(2);
        assertThat(response.getRows().get(0).getSourceCode()).isEqualTo("MAT101");
        verify(courseEquivalencyRepository).deleteByApplication_ApplicationId(3);
        verify(courseEquivalencyRepository).saveAll(any());
    }

    @Test
    void saveTable_conditionsMet_assignsRowOrderCorrectly() {
        Application app = buildApp(4);
        IntibakTableRequest req = new IntibakTableRequest(true, null, List.of(
                buildRow("A101", "CourseA", 3, "A101", "DersA", 3, EquivalencyStatus.TAM_DENKLIK),
                buildRow("B101", "CourseB", 2, "B101", "DersB", 2, EquivalencyStatus.DENK_DEGIL),
                buildRow("C101", "CourseC", 4, "C101", "DersC", 4, EquivalencyStatus.KISMI_DENKLIK)
        ));

        when(applicationRepository.findByApplicationId(4)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any())).thenReturn(app);
        when(courseEquivalencyRepository.saveAll(any())).thenAnswer(inv -> {
            List<CourseEquivalency> saved = inv.getArgument(0);
            assertThat(saved.get(0).getRowOrder()).isEqualTo(0);
            assertThat(saved.get(1).getRowOrder()).isEqualTo(1);
            assertThat(saved.get(2).getRowOrder()).isEqualTo(2);
            return saved;
        });

        courseEquivalencyService.saveTable(4, req);
    }

    @Test
    void saveTable_replacesExistingRows() {
        Application app = buildApp(5);
        IntibakTableRequest req = new IntibakTableRequest(true, null,
                List.of(buildRow("NEW101", "New Course", 3, "NEW101", "Yeni Ders", 3, EquivalencyStatus.TAM_DENKLIK)));

        when(applicationRepository.findByApplicationId(5)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any())).thenReturn(app);
        when(courseEquivalencyRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        courseEquivalencyService.saveTable(5, req);

        verify(courseEquivalencyRepository).deleteByApplication_ApplicationId(5);
        verify(courseEquivalencyRepository).saveAll(any());
    }

    @Test
    void saveTable_tc90_allThreeEquivalencyStatuses_persistedCorrectly() {
        // TC-9.0: TAM_DENKLIK + KISMI_DENKLIK + DENK_DEGIL üçü birlikte doğru kaydedilmeli.
        Application app = buildApp(50);
        IntibakTableRequest req = new IntibakTableRequest(true, null, List.of(
                buildRow("MAT101", "Calculus I",    4, "MATH111", "Calculus I",    4, EquivalencyStatus.TAM_DENKLIK),
                buildRow("FIZ101", "Physics I",     3, "PHYS101", "Physics I",     4, EquivalencyStatus.KISMI_DENKLIK),
                buildRow("BIO100", "Biology",       2, "BIO001",  "Biology Intro", 2, EquivalencyStatus.DENK_DEGIL)
        ));

        when(applicationRepository.findByApplicationId(50)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any())).thenReturn(app);
        when(courseEquivalencyRepository.saveAll(any())).thenAnswer(inv -> {
            List<CourseEquivalency> saved = inv.getArgument(0);
            assertThat(saved).hasSize(3);
            assertThat(saved.get(0).getEquivalencyStatus()).isEqualTo(EquivalencyStatus.TAM_DENKLIK);
            assertThat(saved.get(1).getEquivalencyStatus()).isEqualTo(EquivalencyStatus.KISMI_DENKLIK);
            assertThat(saved.get(2).getEquivalencyStatus()).isEqualTo(EquivalencyStatus.DENK_DEGIL);
            return saved;
        });

        IntibakTableResponse response = courseEquivalencyService.saveTable(50, req);

        assertThat(response.getRows()).hasSize(3);
        verify(courseEquivalencyRepository).deleteByApplication_ApplicationId(50);
        verify(courseEquivalencyRepository).saveAll(any());
    }

    @Test
    void saveTable_conditionsMet_xssSanitizesTextFields() {
        Application app = buildApp(6);
        IntibakTableRequest req = new IntibakTableRequest(true, null, List.of(
                buildRow("<script>alert(1)</script>", "Normal Name", 3, "TGT101", "Hedef", 3, EquivalencyStatus.TAM_DENKLIK)
        ));

        when(applicationRepository.findByApplicationId(6)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any())).thenReturn(app);
        when(courseEquivalencyRepository.saveAll(any())).thenAnswer(inv -> {
            List<CourseEquivalency> saved = inv.getArgument(0);
            assertThat(saved.get(0).getSourceCode()).doesNotContain("<script>");
            return saved;
        });

        courseEquivalencyService.saveTable(6, req);
    }

    // ==========================================
    // SAVE TABLE — satır doğrulama (EX-2: "tablo eksik")
    // ==========================================

    @Test
    void saveTable_conditionsMet_noRows_throwsIllegalArgumentException() {
        Application app = buildApp(8);
        IntibakTableRequest req = new IntibakTableRequest(true, null, List.of());

        when(applicationRepository.findByApplicationId(8)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> courseEquivalencyService.saveTable(8, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("en az bir ders satırı");
        verify(courseEquivalencyRepository, never()).deleteByApplication_ApplicationId(anyInt());
        verify(courseEquivalencyRepository, never()).saveAll(any());
    }

    @Test
    void saveTable_conditionsMet_blankSourceCode_throwsIllegalArgumentException() {
        Application app = buildApp(9);
        IntibakTableRequest req = new IntibakTableRequest(true, null, List.of(
                buildRow("   ", "Calculus", 3, "MAT101", "Matematik", 3, EquivalencyStatus.TAM_DENKLIK)
        ));

        when(applicationRepository.findByApplicationId(9)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> courseEquivalencyService.saveTable(9, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1. satırda");
        verify(courseEquivalencyRepository, never()).saveAll(any());
    }

    @Test
    void saveTable_conditionsMet_nonPositiveCredit_throwsIllegalArgumentException() {
        Application app = buildApp(10);
        IntibakTableRequest req = new IntibakTableRequest(true, null, List.of(
                buildRow("MAT101", "Calculus", 3, "MAT101", "Matematik", 0, EquivalencyStatus.TAM_DENKLIK)
        ));

        when(applicationRepository.findByApplicationId(10)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> courseEquivalencyService.saveTable(10, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pozitif");
        verify(courseEquivalencyRepository, never()).saveAll(any());
    }

    @Test
    void saveTable_conditionsMet_nullEquivalencyStatus_throwsIllegalArgumentException() {
        Application app = buildApp(11);
        IntibakTableRequest req = new IntibakTableRequest(true, null, List.of(
                buildRow("MAT101", "Calculus", 3, "MAT101", "Matematik", 3, null)
        ));

        when(applicationRepository.findByApplicationId(11)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> courseEquivalencyService.saveTable(11, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("denklik durumu");
        verify(courseEquivalencyRepository, never()).saveAll(any());
    }

    // ==========================================
    // SAVE TABLE — koşullar karşılanmıyor
    // ==========================================

    @Test
    void saveTable_conditionsNotMet_savesShortcomingNoteAndSkipsRows() {
        Application app = buildApp(7);
        IntibakTableRequest req = new IntibakTableRequest(false, "Fizik dersi alınmamış.", null);

        when(applicationRepository.findByApplicationId(7)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any())).thenReturn(app);

        IntibakTableResponse response = courseEquivalencyService.saveTable(7, req);

        assertThat(response.getConditionsMet()).isFalse();
        assertThat(response.getShortcomingNote()).isEqualTo("Fizik dersi alınmamış.");
        assertThat(response.getRows()).isEmpty();
        verify(courseEquivalencyRepository, never()).saveAll(any());
    }

    @Test
    void saveTable_conditionsNotMet_missingShortcomingNote_throwsIllegalArgumentException() {
        IntibakTableRequest req = new IntibakTableRequest(false, null, null);

        assertThatThrownBy(() -> courseEquivalencyService.saveTable(1, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shortcomingNote");
    }

    @Test
    void saveTable_conditionsNotMet_blankShortcomingNote_throwsIllegalArgumentException() {
        IntibakTableRequest req = new IntibakTableRequest(false, "   ", null);

        assertThatThrownBy(() -> courseEquivalencyService.saveTable(1, req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saveTable_conditionsMetNull_throwsIllegalArgumentException() {
        IntibakTableRequest req = new IntibakTableRequest(null, null, null);

        assertThatThrownBy(() -> courseEquivalencyService.saveTable(1, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conditionsMet");
    }

    @Test
    void saveTable_applicationNotFound_throwsEntityNotFoundException() {
        IntibakTableRequest req = new IntibakTableRequest(true, null, List.of());
        when(applicationRepository.findByApplicationId(anyInt())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseEquivalencyService.saveTable(99, req))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==========================================
    // TEK SATIR EKLEME — addRow()
    // ==========================================

    @Test
    void addRow_valid1to1_appendsRow() {
        Application app = buildApp(20);
        app.setYgkApproved(true);
        CourseEquivalencyRow row = buildRow("MAT101", "Calculus", 3, "MAT201", "Matematik", 3, EquivalencyStatus.TAM_DENKLIK);
        CourseEquivalency saved = buildEntity(app, "MAT101", "Calculus", 3, "MAT201", "Matematik", 3, EquivalencyStatus.TAM_DENKLIK, 0);

        when(applicationRepository.findByApplicationId(20)).thenReturn(Optional.of(app));
        when(courseEquivalencyRepository.countByApplication_ApplicationId(20)).thenReturn(0);
        when(courseEquivalencyRepository.save(any())).thenReturn(saved);
        when(courseEquivalencyRepository.findByApplication_ApplicationIdOrderByRowOrderAsc(20))
                .thenReturn(List.of(saved));

        IntibakTableResponse response = courseEquivalencyService.addRow(20, row);

        assertThat(response.getRows()).hasSize(1);
        assertThat(response.getRows().get(0).getSourceCode()).isEqualTo("MAT101");
        verify(courseEquivalencyRepository).save(any());
    }

    @Test
    void addRow_valid2to1_appendsRow() {
        Application app = buildApp(21);
        app.setYgkApproved(true);
        CourseEquivalencyRow row = CourseEquivalencyRow.builder()
                .sourceCode("MAT101").sourceName("Probability").sourceCredit(3).sourceGrade("BB")
                .source2Code("MAT102").source2Name("Statistics").source2Credit(3).source2Grade("CC")
                .targetCode("MAT201").targetName("Probability and Statistics").targetCredit(4).targetGrade("CB")
                .equivalencyStatus(EquivalencyStatus.TAM_DENKLIK)
                .build();
        CourseEquivalency saved = CourseEquivalency.builder()
                .id(1).application(app)
                .sourceCode("MAT101").sourceName("Probability").sourceCredit(3).sourceGrade("BB")
                .source2Code("MAT102").source2Name("Statistics").source2Credit(3).source2Grade("CC")
                .targetCode("MAT201").targetName("Probability and Statistics").targetCredit(4).targetGrade("CB")
                .equivalencyStatus(EquivalencyStatus.TAM_DENKLIK).rowOrder(0)
                .build();

        when(applicationRepository.findByApplicationId(21)).thenReturn(Optional.of(app));
        when(courseEquivalencyRepository.countByApplication_ApplicationId(21)).thenReturn(0);
        when(courseEquivalencyRepository.save(any())).thenReturn(saved);
        when(courseEquivalencyRepository.findByApplication_ApplicationIdOrderByRowOrderAsc(21))
                .thenReturn(List.of(saved));

        IntibakTableResponse response = courseEquivalencyService.addRow(21, row);

        assertThat(response.getRows()).hasSize(1);
        assertThat(response.getRows().get(0).getSource2Code()).isEqualTo("MAT102");
        verify(courseEquivalencyRepository).save(any());
    }

    @Test
    void addRow_partial2to1_throwsValidation() {
        Application app = buildApp(22);
        // source2Name provided but source2Code, source2Credit, source2Grade missing
        CourseEquivalencyRow row = CourseEquivalencyRow.builder()
                .sourceCode("MAT101").sourceName("Calculus").sourceCredit(3).sourceGrade("BB")
                .source2Name("Statistics")
                .targetCode("MAT201").targetName("Matematik").targetCredit(3).targetGrade("BB")
                .equivalencyStatus(EquivalencyStatus.TAM_DENKLIK)
                .build();

        when(applicationRepository.findByApplicationId(22)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> courseEquivalencyService.addRow(22, row))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tamamı dolu ya da tamamı boş");
        verify(courseEquivalencyRepository, never()).save(any());
    }

    @Test
    void addRow_invalidGrade_throwsValidation() {
        Application app = buildApp(23);
        CourseEquivalencyRow row = CourseEquivalencyRow.builder()
                .sourceCode("MAT101").sourceName("Calculus").sourceCredit(3).sourceGrade("XY")
                .targetCode("MAT201").targetName("Matematik").targetCredit(3).targetGrade("BB")
                .equivalencyStatus(EquivalencyStatus.TAM_DENKLIK)
                .build();

        when(applicationRepository.findByApplicationId(23)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> courseEquivalencyService.addRow(23, row))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("geçersiz");
        verify(courseEquivalencyRepository, never()).save(any());
    }

    // ==========================================
    // SATIR DÜZENLEME — editRow()
    // ==========================================

    @Test
    void editRow_wrongApplication_throwsValidation() {
        Application app = buildApp(24);
        CourseEquivalencyRow row = buildRow("MAT101", "Calculus", 3, "MAT201", "Matematik", 3, EquivalencyStatus.TAM_DENKLIK);

        when(applicationRepository.findByApplicationId(24)).thenReturn(Optional.of(app));
        when(courseEquivalencyRepository.findByIdAndApplication_ApplicationId(99, 24))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseEquivalencyService.editRow(24, 99, row))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bu başvuruya ait değil");
        verify(courseEquivalencyRepository, never()).save(any());
    }

    // ==========================================
    // SATIR SİLME — deleteRow()
    // ==========================================

    @Test
    void deleteRow_removesRow_ratioUpdates() {
        Application app = buildApp(25);
        app.setYgkApproved(true);
        CourseEquivalency existing = buildEntity(app, "MAT101", "Calculus", 3, "MAT201", "Matematik", 3, EquivalencyStatus.TAM_DENKLIK, 0);
        existing = CourseEquivalency.builder()
                .id(5).application(app)
                .sourceCode("MAT101").sourceName("Calculus").sourceCredit(3).sourceGrade("BB")
                .targetCode("MAT201").targetName("Matematik").targetCredit(3).targetGrade("BB")
                .equivalencyStatus(EquivalencyStatus.TAM_DENKLIK).rowOrder(0)
                .build();

        when(applicationRepository.findByApplicationId(25)).thenReturn(Optional.of(app));
        when(courseEquivalencyRepository.findByIdAndApplication_ApplicationId(5, 25))
                .thenReturn(Optional.of(existing));
        when(courseEquivalencyRepository.findByApplication_ApplicationIdOrderByRowOrderAsc(25))
                .thenReturn(List.of());

        IntibakTableResponse response = courseEquivalencyService.deleteRow(25, 5);

        verify(courseEquivalencyRepository).delete(existing);
        assertThat(response.getRows()).isEmpty();
        assertThat(response.getEquivalencyRatio()).isNull();
    }

    // ==========================================
    // %80 DENKLİK ORANI — calculateEquivalencyRatio()
    // ==========================================

    @Test
    void calculateRatio_allEquivalent_returns1() {
        Application app = buildApp(30);
        app.setYgkApproved(true);
        List<CourseEquivalency> rows = List.of(
                buildEntity(app, "A", "A", 4, "A", "A", 4, EquivalencyStatus.TAM_DENKLIK, 0),
                buildEntity(app, "B", "B", 6, "B", "B", 6, EquivalencyStatus.KISMI_DENKLIK, 1)
        );

        when(applicationRepository.findByApplicationId(30)).thenReturn(Optional.of(app));
        when(courseEquivalencyRepository.findByApplication_ApplicationIdOrderByRowOrderAsc(30)).thenReturn(rows);

        assertThat(courseEquivalencyService.calculateEquivalencyRatio(30)).isEqualTo(1.0);
    }

    @Test
    void calculateRatio_noneEquivalent_returns0() {
        Application app = buildApp(31);
        app.setYgkApproved(true);
        List<CourseEquivalency> rows = List.of(
                buildEntity(app, "A", "A", 3, "A", "A", 3, EquivalencyStatus.DENK_DEGIL, 0),
                buildEntity(app, "B", "B", 4, "B", "B", 4, EquivalencyStatus.DENK_DEGIL, 1)
        );

        when(applicationRepository.findByApplicationId(31)).thenReturn(Optional.of(app));
        when(courseEquivalencyRepository.findByApplication_ApplicationIdOrderByRowOrderAsc(31)).thenReturn(rows);

        assertThat(courseEquivalencyService.calculateEquivalencyRatio(31)).isEqualTo(0.0);
    }

    @Test
    void calculateRatio_mixed_correctRatio() {
        Application app = buildApp(32);
        app.setYgkApproved(true);
        // 6 equivalent credits out of 10 total → 0.6
        List<CourseEquivalency> rows = List.of(
                buildEntity(app, "A", "A", 6, "A", "A", 6, EquivalencyStatus.TAM_DENKLIK, 0),
                buildEntity(app, "B", "B", 4, "B", "B", 4, EquivalencyStatus.DENK_DEGIL, 1)
        );

        when(applicationRepository.findByApplicationId(32)).thenReturn(Optional.of(app));
        when(courseEquivalencyRepository.findByApplication_ApplicationIdOrderByRowOrderAsc(32)).thenReturn(rows);

        assertThat(courseEquivalencyService.calculateEquivalencyRatio(32)).isEqualTo(0.6);
    }

    @Test
    void calculateRatio_kismiDenklik_countedAsEquivalent_notAsDenkDegil() {
        // TC-9.0: KISMI_DENKLIK kısmen denk sayılır → orana dahil edilir (DENK_DEGIL gibi sıfır saymaz).
        // 4 kredi KISMI_DENKLIK + 4 kredi DENK_DEGIL → 4/8 = 0.5
        Application app = buildApp(36);
        app.setYgkApproved(true);
        List<CourseEquivalency> rows = List.of(
                buildEntity(app, "FIZ101", "Physics I", 4, "PHYS101", "Physics I", 4, EquivalencyStatus.KISMI_DENKLIK, 0),
                buildEntity(app, "BIO100", "Biology",   4, "",        "",          0, EquivalencyStatus.DENK_DEGIL,   1)
        );

        when(applicationRepository.findByApplicationId(36)).thenReturn(Optional.of(app));
        when(courseEquivalencyRepository.findByApplication_ApplicationIdOrderByRowOrderAsc(36)).thenReturn(rows);

        assertThat(courseEquivalencyService.calculateEquivalencyRatio(36)).isEqualTo(0.5);
    }

    @Test
    void calculateRatio_2to1group_usesCombinedCredits() {
        Application app = buildApp(33);
        app.setYgkApproved(true);
        // 2→1 group: source1=3 + source2=3 = 6 combined credits, TAM_DENKLIK → 6/6 = 1.0
        CourseEquivalency group = CourseEquivalency.builder()
                .id(1).application(app)
                .sourceCode("MAT101").sourceName("Probability").sourceCredit(3).sourceGrade("BB")
                .source2Code("MAT102").source2Name("Statistics").source2Credit(3).source2Grade("CC")
                .targetCode("MAT201").targetName("Prob&Stats").targetCredit(4).targetGrade("CB")
                .equivalencyStatus(EquivalencyStatus.TAM_DENKLIK).rowOrder(0)
                .build();

        when(applicationRepository.findByApplicationId(33)).thenReturn(Optional.of(app));
        when(courseEquivalencyRepository.findByApplication_ApplicationIdOrderByRowOrderAsc(33))
                .thenReturn(List.of(group));

        assertThat(courseEquivalencyService.calculateEquivalencyRatio(33)).isEqualTo(1.0);
    }

    @Test
    void calculateRatio_emptyTable_returnsNull() {
        Application app = buildApp(34);
        app.setYgkApproved(true);

        when(applicationRepository.findByApplicationId(34)).thenReturn(Optional.of(app));
        when(courseEquivalencyRepository.findByApplication_ApplicationIdOrderByRowOrderAsc(34))
                .thenReturn(List.of());

        assertThat(courseEquivalencyService.calculateEquivalencyRatio(34)).isNull();
    }

    @Test
    void calculateRatio_conditionsNotMet_returnsNull() {
        Application app = buildApp(35);
        app.setYgkApproved(false);

        when(applicationRepository.findByApplicationId(35)).thenReturn(Optional.of(app));

        assertThat(courseEquivalencyService.calculateEquivalencyRatio(35)).isNull();
    }

    // ==========================================
    // YARDIMCI METOTLAR
    // ==========================================

    private Application buildApp(Integer id) {
        return Application.builder()
                .applicationId(id)
                .status(ApplicationStatus.YGK_SCORED)
                .gpa(3.0)
                .sayYksScore(400.0)
                .build();
    }

    private CourseEquivalencyRow buildRow(String srcCode, String srcName, int srcCredit,
                                          String tgtCode, String tgtName, int tgtCredit,
                                          EquivalencyStatus status) {
        return CourseEquivalencyRow.builder()
                .sourceCode(srcCode).sourceName(srcName).sourceCredit(srcCredit).sourceGrade("BB")
                .targetCode(tgtCode).targetName(tgtName).targetCredit(tgtCredit).targetGrade("BB")
                .equivalencyStatus(status)
                .build();
    }

    private CourseEquivalency buildEntity(Application app, String srcCode, String srcName, int srcCredit,
                                          String tgtCode, String tgtName, int tgtCredit,
                                          EquivalencyStatus status, int order) {
        return CourseEquivalency.builder()
                .application(app)
                .sourceCode(srcCode).sourceName(srcName).sourceCredit(srcCredit).sourceGrade("BB")
                .targetCode(tgtCode).targetName(tgtName).targetCredit(tgtCredit).targetGrade("BB")
                .equivalencyStatus(status)
                .rowOrder(order)
                .build();
    }
}
