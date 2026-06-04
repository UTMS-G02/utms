package edu.iztech.utms.g02.utms_app.bl.evaluation;

/**
 * Composite score hesaplaması — Pair 3 değerlendirme formülü.
 *
 * <p>Formül sabittir ve istek gövdesinden (request body) ASLA okunmaz:
 * <pre>compositeScore = (0.10 × GPA) + (0.90 × yksScore)</pre>
 *
 * Ağırlıklar yalnızca burada, {@code private static final} sabitler olarak tutulur.
 */
public final class ScoreCalculator {

    private static final double GPA_WEIGHT = 0.10;
    private static final double YKS_WEIGHT = 0.90;

    private ScoreCalculator() {
        // Utility sınıfı — örneklenmez.
    }

    public static double calculate(double gpa, double yksScore) {
        return (GPA_WEIGHT * gpa) + (YKS_WEIGHT * yksScore);
    }
}
