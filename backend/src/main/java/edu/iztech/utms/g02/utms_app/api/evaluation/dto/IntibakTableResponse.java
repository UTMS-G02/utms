package edu.iztech.utms.g02.utms_app.api.evaluation.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class IntibakTableResponse {

    private Integer applicationId;
    private Boolean conditionsMet;
    private String shortcomingNote;
    private List<CourseEquivalencyRow> rows;
}
