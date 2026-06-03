package edu.iztech.utms.g02.utms_app.api.evaluation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class IntibakTableRequest {

    // true = Koşullar Karşılanıyor, false = Koşullar Karşılanmıyor
    private Boolean conditionsMet;

    // Koşullar karşılanmıyorsa zorunlu
    private String shortcomingNote;

    // Koşullar karşılanıyorsa dolu gelir; karşılanmıyorsa boş/null
    private List<CourseEquivalencyRow> rows;
}
