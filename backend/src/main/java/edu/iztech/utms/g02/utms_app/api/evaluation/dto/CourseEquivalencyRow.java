package edu.iztech.utms.g02.utms_app.api.evaluation.dto;

import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.EquivalencyStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseEquivalencyRow {

    private String sourceCode;
    private String sourceName;
    private Integer sourceCredit;

    private String targetCode;
    private String targetName;
    private Integer targetCredit;

    private EquivalencyStatus equivalencyStatus;
}
