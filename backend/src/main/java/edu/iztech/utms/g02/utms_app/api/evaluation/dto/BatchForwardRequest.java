package edu.iztech.utms.g02.utms_app.api.evaluation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * TC-10.7: Dekanlık toplu iletim isteği.
 * ids: iletilecek başvuru ID listesi
 * action: TO_YGK | TO_FACULTY_BOARD | TO_OIDB
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchForwardRequest {

    public enum Action { TO_YGK, TO_FACULTY_BOARD, TO_OIDB }

    private List<Integer> ids;
    private Action action;
}
