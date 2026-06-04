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
                .sourceCode(srcCode).sourceName(srcName).sourceCredit(srcCredit)
                .targetCode(tgtCode).targetName(tgtName).targetCredit(tgtCredit)
                .equivalencyStatus(status)
                .build();
    }

    private CourseEquivalency buildEntity(Application app, String srcCode, String srcName, int srcCredit,
                                          String tgtCode, String tgtName, int tgtCredit,
                                          EquivalencyStatus status, int order) {
        return CourseEquivalency.builder()
                .application(app)
                .sourceCode(srcCode).sourceName(srcName).sourceCredit(srcCredit)
                .targetCode(tgtCode).targetName(tgtName).targetCredit(tgtCredit)
                .equivalencyStatus(status)
                .rowOrder(order)
                .build();
    }
}
