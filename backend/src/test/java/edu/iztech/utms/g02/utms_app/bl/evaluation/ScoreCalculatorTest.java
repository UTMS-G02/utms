package edu.iztech.utms.g02.utms_app.bl.evaluation;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreCalculatorTest {

    @Test
    void calculate_withKnownValues_appliesFixedWeights() {
        // 0.10 * 3.50 + 0.90 * 400 = 0.35 + 360 = 360.35
        double score = ScoreCalculator.calculate(3.50, 400.0);

        assertThat(score).isCloseTo(360.35, Offset.offset(0.001));
    }

    @Test
    void calculate_gpaWeightIsFixed_gpaCanShiftScoreAtMostByPointFour() {
        // GPA ağırlığı 0.10 olduğundan, YKS sabitken GPA'nın yaratabileceği
        // maksimum fark = 0.10 * 4.0 = 0.40
        double highGpa = ScoreCalculator.calculate(4.0, 300.0);
        double lowGpa = ScoreCalculator.calculate(0.0, 300.0);

        assertThat(highGpa - lowGpa).isLessThanOrEqualTo(0.40);
    }

    @Test
    void calculate_yksDominatesScore() {
        // YKS ağırlığı 0.90 → skoru büyük oranda YKS belirler
        double lowYks = ScoreCalculator.calculate(4.0, 100.0);
        double highYks = ScoreCalculator.calculate(4.0, 500.0);

        assertThat(highYks - lowYks).isCloseTo(360.0, Offset.offset(0.001));
    }
}
