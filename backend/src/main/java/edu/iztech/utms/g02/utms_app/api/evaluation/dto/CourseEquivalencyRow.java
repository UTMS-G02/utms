package edu.iztech.utms.g02.utms_app.api.evaluation.dto;

import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.EquivalencyStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseEquivalencyRow {

    private Integer id;

    // Kaynak ders 1 (zorunlu)
    private String sourceCode;
    private String sourceName;
    private Integer sourceCredit;
    private String sourceGrade;

    // Kaynak ders 2 (yalnızca 2→1 eşleştirmede; hepsi birlikte null veya dolu)
    private String source2Code;
    private String source2Name;
    private Integer source2Credit;
    private String source2Grade;

    // Hedef ders (zorunlu)
    private String targetCode;
    private String targetName;
    private Integer targetCredit;
    private String targetGrade;

    private EquivalencyStatus equivalencyStatus;
}
