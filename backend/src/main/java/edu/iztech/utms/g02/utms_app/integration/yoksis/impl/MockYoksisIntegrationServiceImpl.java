package edu.iztech.utms.g02.utms_app.integration.yoksis.impl;

import edu.iztech.utms.g02.utms_app.integration.yoksis.YoksisIntegrationService;
import edu.iztech.utms.g02.utms_app.integration.yoksis.dto.YoksisStudentResponse;
import org.springframework.stereotype.Service;

@Service
public class MockYoksisIntegrationServiceImpl implements YoksisIntegrationService {

    // {üniversite, fakülte, bölüm} — fakülte ve bölüm HER ZAMAN tutarlı (kurallara
    // uygun) eşleşir; öğrenciye TCKN'inden deterministik olarak bir satır atanır.
    private static final String[][] ACADEMIC_POOL = {
        {"Ege Üniversitesi", "Mühendislik Fakültesi", "Bilgisayar Mühendisliği"},
        {"Dokuz Eylül Üniversitesi", "Mühendislik Fakültesi", "Elektrik-Elektronik Mühendisliği"},
        {"İstanbul Teknik Üniversitesi", "Makine Fakültesi", "Makine Mühendisliği"},
        {"Orta Doğu Teknik Üniversitesi", "Mühendislik Fakültesi", "Endüstri Mühendisliği"},
        {"Boğaziçi Üniversitesi", "Mühendislik Fakültesi", "Kimya Mühendisliği"},
        {"Hacettepe Üniversitesi", "Mimarlık Fakültesi", "Mimarlık"},
        {"Yıldız Teknik Üniversitesi", "Mimarlık Fakültesi", "Şehir ve Bölge Planlama"},
    };

    // Test TCKNs with fixed profiles — always return the same data regardless of hash.
    // "99999999999" → yksRank=380,000 (above 300,000 Mühendislik limit) → application blocked.
    private static final java.util.Map<String, YoksisStudentResponse> OVERRIDES =
        java.util.Map.of(
            "99999999999", new YoksisStudentResponse(
                "Ege Üniversitesi", "Mühendislik Fakültesi", "Bilgisayar Mühendisliği",
                4, 3.20, 280.0, 380_000, 4)
        );

    // Central mock source. ÖĞRENCİYE ÖZEL ve DETERMİNİSTİK: aynı TCKN her zaman aynı
    // akademik veriyi (okul/fakülte/bölüm/GPA/YKS) döndürür; veri başvurudan başvuruya
    // veya çağrıdan çağrıya DEĞİŞMEZ. Böylece form (GET /yoksis/me) ile kayıt (create())
    // her zaman birebir aynı veriyi görür. Farklı öğrenciler ise farklı profiller alır
    // (bazıları GPA/sıralama barajını geçer, bazıları kalır). Gerçek YÖKSİS yok.
    @Override
    public YoksisStudentResponse fetchAcademicDataByTckn(String tckn) {
        YoksisStudentResponse override = OVERRIDES.get(tckn);
        if (override != null) return override;
        // TCKN'den türetilen sabit tohum → tüm alanlar bundan deterministik üretilir.
        // Alanları birbirinden bağımsız tutmak için tohumun farklı dilimleri kullanılır
        // (ör. sınıf ile GPA ölçeği korelasyonlu çıkmasın).
        //int seed = (tckn == null ? "" : tckn).hashCode() & 0x7fffffff;

        // 1. TCKN'den ham hash değerini al.
        int rawHash = (tckn == null ? "" : tckn).hashCode();

        // 2. Hash'in bitlerini kendi üstüne kaydırarak (XOR) karıştır. 
        // Bu sayede her zaman çift sayı gelme zincirini kırıyoruz.
        int mixedHash = rawHash ^ (rawHash >>> 16);

        // 3. Negatif sayıları pozitif yapmak için maskele.
        int seed = mixedHash & 0x7fffffff;

        // Üniversite + (tutarlı) fakülte/bölüm kombinasyonu.
        String[] row = ACADEMIC_POOL[(seed / 4) % ACADEMIC_POOL.length];

        // Sınıf 1–4 → tamamlanan yarıyıl 2/4/6/8. Yatay geçiş yalnızca 1. (yarıyıl 2)
        // ve 2. (yarıyıl 4) sınıflara açıktır; 3./4. sınıf (yarıyıl 6/8) backend'de
        // reddedilir. Bu yüzden mock bilinçli olarak 3./4. sınıf da üretir.
        int classYear = 1 + (seed % 4);   // 1, 2, 3, 4
        int semester = classYear * 2;     // 2, 4, 6, 8 (tamamlanan yarıyıl)

        // GPA: ~%25 öğrenci 100'lük, aksi halde 4'lük sistem. Aralıklar bilinçli olarak
        // barajın HEM altını HEM üstünü kapsar (kimi öğrenci geçer, kimi kalır).
        int gpaScale;
        double gpa;
        if ((seed / 28) % 4 == 0) {
            gpaScale = 100;
            gpa = round2(50.0 + (seed % 4500) / 100.0);   // 50.00 – 94.99 (baraj: 70)
        } else {
            gpaScale = 4;
            gpa = round2(1.50 + (seed % 250) / 100.0);     // 1.50 – 3.99 (baraj: 2.50)
        }

        // ÖSYM (mock): SAY puanı ~380–499.99. Sıralama HER ZAMAN uygun gelsin diye en katı
        // baraj olan Mimarlık (<=250.000) eşiğinin altında üretilir (1.000 – 240.999) →
        // hem Mühendislik (<=300.000) hem Mimarlık başvuruları sıralama barajını geçer.
        double yksScore = round2(380.0 + (seed % 12000) / 100.0);
        int yksRank = 1000 + (seed % 240_000);

        return new YoksisStudentResponse(row[0], row[1], row[2], semester, gpa, yksScore, yksRank, gpaScale);
    }

    // GPA / puan değerlerini 2 ondalığa yuvarla (formdaki precision=2 ile uyumlu).
    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
