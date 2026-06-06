package edu.iztech.utms.g02.utms_app.integration.yoksis;

/**
 * YÖKSİS'ten gelen not ortalamasını sistemin tek standardı olan 4'lük sisteme çevirir.
 *
 * <p>Bazı üniversiteler GPA'yı 100'lük sistemde tutar; UTMS ise her yerde (kayıt,
 * doğrulama, frontend) 4'lük ölçeği kullanır. Bu yüzden 100'lük gelen değerler bu
 * yardımcı ile <b>sınırda</b> (YÖKSİS verisi sisteme girerken) 4'lüğe normalize edilir.
 *
 * <p>Dönüşüm doğrusaldır ve {@code 70/100 -> 2.50/4.00}, {@code 100/100 -> 4.00/4.00}
 * noktalarından geçer (kurum içi yatay geçiş barajı 2.50 ≡ 70 denkliğiyle uyumlu).
 * Sonuç {@code [0.00, 4.00]} aralığına sıkıştırılır. 4'lük (veya bilinmeyen) ölçekteki
 * değerler olduğu gibi (yalnızca aralığa sıkıştırılarak) döner.
 */
public final class GpaScaleConverter {

    private GpaScaleConverter() { }

    public static double toFourScale(Double gpa, Integer scale) {
        if (gpa == null) {
            return 0.0;
        }
        if (scale != null && scale == 100) {
            // (70,2.50)-(100,4.00) doğrusu: eğim = 1.5/30 = 0.05
            double converted = 2.50 + (gpa - 70.0) * 0.05;
            return round2(clamp(converted));
        }
        // Zaten 4'lük (veya ölçek belirtilmemiş) → olduğu gibi (aralığa sıkıştırarak).
        return round2(clamp(gpa));
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(4.0, v));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
