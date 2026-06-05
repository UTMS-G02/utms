package edu.iztech.utms.g02.utms_app.api.evaluation.controller;

import edu.iztech.utms.g02.utms_app.api.evaluation.dto.CourseEquivalencyRow;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.IntibakTableRequest;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.IntibakTableResponse;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.EquivalencyStatus;
import edu.iztech.utms.g02.utms_app.bl.evaluation.CourseEquivalencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseEquivalencyControllerTest {

    @Mock private CourseEquivalencyService courseEquivalencyService;

    @InjectMocks private CourseEquivalencyController controller;

    // ==========================================
    // GET /api/applications/{id}/intibak
    // ==========================================

    @Test
    void getTable_delegatesToService_returnsResponse() {
        IntibakTableResponse expected = IntibakTableResponse.builder()
                .applicationId(1).conditionsMet(true).rows(List.of()).equivalencyRatio(1.0).build();
        when(courseEquivalencyService.getTable(1)).thenReturn(expected);

        ResponseEntity<IntibakTableResponse> response = controller.getTable(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getApplicationId()).isEqualTo(1);
        assertThat(response.getBody().getEquivalencyRatio()).isEqualTo(1.0);
        verify(courseEquivalencyService).getTable(1);
    }

    // ==========================================
    // PUT /api/applications/{id}/intibak
    // ==========================================

    @Test
    void saveTable_delegatesToService_returnsResponse() {
        IntibakTableRequest req = new IntibakTableRequest(true, null, List.of());
        IntibakTableResponse expected = IntibakTableResponse.builder()
                .applicationId(2).conditionsMet(true).rows(List.of()).equivalencyRatio(null).build();
        when(courseEquivalencyService.saveTable(eq(2), eq(req))).thenReturn(expected);

        ResponseEntity<IntibakTableResponse> response = controller.saveTable(2, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getApplicationId()).isEqualTo(2);
        verify(courseEquivalencyService).saveTable(2, req);
    }

    // ==========================================
    // POST /api/applications/{id}/intibak/rows
    // ==========================================

    @Test
    void addRow_delegatesToService_returnsUpdatedTable() {
        CourseEquivalencyRow row = buildRow();
        IntibakTableResponse expected = IntibakTableResponse.builder()
                .applicationId(3).conditionsMet(true)
                .rows(List.of(row)).equivalencyRatio(1.0).build();
        when(courseEquivalencyService.addRow(eq(3), any())).thenReturn(expected);

        ResponseEntity<IntibakTableResponse> response = controller.addRow(3, row);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getRows()).hasSize(1);
        assertThat(response.getBody().getEquivalencyRatio()).isEqualTo(1.0);
        verify(courseEquivalencyService).addRow(eq(3), any());
    }

    // ==========================================
    // PATCH /api/applications/{id}/intibak/rows/{rowId}
    // ==========================================

    @Test
    void editRow_delegatesToService_returnsUpdatedTable() {
        CourseEquivalencyRow row = buildRow();
        IntibakTableResponse expected = IntibakTableResponse.builder()
                .applicationId(4).conditionsMet(true)
                .rows(List.of(row)).equivalencyRatio(0.75).build();
        when(courseEquivalencyService.editRow(eq(4), eq(10), any())).thenReturn(expected);

        ResponseEntity<IntibakTableResponse> response = controller.editRow(4, 10, row);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getEquivalencyRatio()).isEqualTo(0.75);
        verify(courseEquivalencyService).editRow(eq(4), eq(10), any());
    }

    // ==========================================
    // DELETE /api/applications/{id}/intibak/rows/{rowId}
    // ==========================================

    @Test
    void deleteRow_delegatesToService_returnsUpdatedTable() {
        IntibakTableResponse expected = IntibakTableResponse.builder()
                .applicationId(5).conditionsMet(true)
                .rows(List.of()).equivalencyRatio(null).build();
        when(courseEquivalencyService.deleteRow(5, 20)).thenReturn(expected);

        ResponseEntity<IntibakTableResponse> response = controller.deleteRow(5, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getRows()).isEmpty();
        verify(courseEquivalencyService).deleteRow(5, 20);
    }

    // ==========================================
    // YARDIMCI METOTLAR
    // ==========================================

    private CourseEquivalencyRow buildRow() {
        return CourseEquivalencyRow.builder()
                .sourceCode("MAT101").sourceName("Calculus").sourceCredit(3).sourceGrade("BB")
                .targetCode("MAT201").targetName("Matematik").targetCredit(3).targetGrade("BB")
                .equivalencyStatus(EquivalencyStatus.TAM_DENKLIK)
                .build();
    }
}
