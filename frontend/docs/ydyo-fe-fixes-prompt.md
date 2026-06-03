# YDYO Paneli — Frontend Düzeltmeleri (test case'lere hizalama)

> Bu düzeltmeler `docs/ydyo-test-cases.md`'deki UC-7 case'lerini geçirmek için.
> **Sadece frontend + mock (`src/api/ydyo.js`).** Backend'e DOKUNMA — backend gap'leri ayrı, Arda'da.
> Gerçek API çağrıları TODO olarak kalsın; mock davranışı test'lerle eşleşmeli.
> Tüm Türkçe mesajlar **birebir** olmalı. Sırayla ilerle.

---

## 1. Detay ekranı — dinamik "Sınav Sonucu" dropdown'u (EN ÖNEMLİ) — TC-7.0 / 7.3 / 7.9 / 7.10

`YdyoApplicationDetail.jsx`'te şu an sınav sonucu read-only önizleme (DECISION_PREVIEW) ve giriş ayrı toplu sayfada. Test'ler **detayda tek ekranda dinamik akış** istiyor. Düzelt:

- **"Belge Onay Durumu"** dropdown'undan **"Reddedildi" seçeneğini kaldır.** Sadece 2 seçenek: `Onaylandı` / `Onaylanmadı`.
- **`Onaylandı` seçilince:**
  - `Sınav Sonucu` = `Sınav Gerekli Değil` (pasif/locked, seçilemez).
  - `Muafiyet Sonucu` = `Muaf` (read-only).
- **`Onaylanmadı` seçilince:**
  - `Sınav Sonucu` **aktif dropdown** olur, seçenekler: `Sınav Sonucu Bekleniyor` / `Sınav Başarılı` / `Sınav Başarısız`.
  - `Muafiyet Sonucu` otomatik türenir: `Sınav Sonucu Bekleniyor` → `Beklemede`, `Sınav Başarılı` → `Muaf`, `Sınav Başarısız` → `Muaf Değil`.
- **`Muafiyet Sonucu` her durumda read-only** (greyed out, tıklanamaz, elle override edilemez).
- **`Açıklama` zorunlu:** boşken Kaydet → `Lütfen açıklama giriniz.` (zaten var, koru).
- **Kaydet mock mapping'i** (gerçeği TODO):
  - `Onaylandı` → `ydyo-initial-review` `{ approved: true, requiresExam: false, notes, reviewerId }`
  - `Onaylanmadı` + `Sınav Sonucu Bekleniyor` → `ydyo-initial-review` `{ approved: false, requiresExam: true, notes, reviewerId }`
  - `Onaylanmadı` + `Sınav Başarılı/Başarısız` → `ydyo-exam-result` `{ passed: true/false, examScore: null, notes, reviewerId }`

> Toplu CSV sayfası kalsın (bireysel detay girişi + toplu CSV birlikte var olacak — test ikisini de bekliyor).

---

## 2. Toplu CSV — kolon kontratı ve akış — TC-7.4 / 7.5

`YdyoExamResults.jsx` `parseCsv`'i düzelt:

- **Kolonlar birebir:** `studentId`, `score`, `result`. `studentId` başlığını tanı (applicationid/id/başvuru no arama mantığını kaldır).
- **Pass/fail'i `result` kolonu belirler**, score eşiği DEĞİL: `result` = `Sınav Başarılı` → passed=true (`Muaf`), `Sınav Başarısız` → passed=false (`Muaf Değil`). `score` sadece `examScore` olarak saklanır.
- **Akış:** yükleyince **doğrudan işle** (ilerleme göstergesi göster), öndoldur-sonra-elle-Kaydet adımını kaldır.
- **studentId → application eşleşmesi** yüklü listeden yapılır. Her satır için `ydyo-exam-result` PATCH (mock; gerçek TODO).
- **Özet** (işlem sonu): `Toplam: N · Başarılı (Muaf): X · Başarısız (Muaf Değil): Y · Hatalı: Z`. (7.4'te Z=0; 7.5'te Muaf / Muaf Değil sayıları görünür.)

---

## 3. Toplu CSV — negatif durumlar — TC-7.6

- **Non-CSV dosya:** `Geçersiz dosya formatı. Lütfen bir .csv dosyası yükleyiniz.` (zaten var, koru).
- **Eksik zorunlu kolon** (studentId / score / result yoksa): **şu an tetiklenmiyor**, ekle → `Geçersiz dosya yapısı. Lütfen zorunlu sütunların (studentId, score, result) mevcut olduğunu kontrol ediniz.` Hiçbir kayıt güncellenmez.
- **Tanınmayan studentId:** satır atlanır VE **final özetteki "Hatalı" sayısına yansır** (şu an yükleme mesajında "eşleşmedi" var ama özete girmiyor).
- **Boş satırlar:** atlanır, özet atlanan/hatalı sayısını yansıtır.

---

## 4. ÖİDB'ye İlet — mesaj + statü — TC-7.7

- **Başarı mesajını birebir yap:** mevcut `Sonuçlar iletildi.` → `Liste başarıyla ÖİDB'ye iletildi.`
- **İlet sonrası mock'ta** tüm finalize kayıtları "iletildi/tamamlandı" olarak işaretle ve listeyi kilitle (statü görünümü `YDYO Completed`). 
  > Gerçek statü geçişi backend'de yok (Arda'da) → `// TODO: real YDYO_COMPLETED transition via backend transmit endpoint` notu bırak. Şimdilik mock'ta simüle et.
- `Beklemede` guard + `Tüm öğrenci kayıtları tamamlanmadan liste iletilemez.` zaten doğru, koru (TC-7.8).

---

## Sonuç
Sırayla 1→4 git. Her adımda mevcut student panel stiliyle tutarlılığı koru, backend'e dokunma, gerçek API'yi TODO bırak. Bitince hangi test case'lerin artık karşılandığını kısaca özetle.
