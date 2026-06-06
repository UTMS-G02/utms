package edu.iztech.utms.g02.utms_app.api.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TC-10.7: Dekanlık toplu iletim yanıtı.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchForwardResponse {

    private int forwarded;
}
