# UC-7 — YDYO (Manage English Proficiency Assessment) Test Case'leri

> Bu, graders'ın YDYO panelini test edeceği case'lerin tam listesi — **son implementasyona göre**. Tüm Türkçe UI metinleri (buton/etiket/mesaj) **birebir** eşleşmeli.

## Test öğrencileri (sabit veri)

Panel sabit test verisiyle gelir (`frontend/src/api/ydyo.js`). Her case ilgili öğrenciyi **isimle** seçer:

| Öğrenci | Başlangıç durumu | İngilizce belgesi | Kullanıldığı case'ler |
|---|---|---|---|
| **Ayşe Demir** | Değerlendirilmemiş (taze) | TOEFL (geçerli) | TC-7.0, 7.1, 7.9, 7.11 |
| **Burak Çelik** | Değerlendirilmemiş (taze) | yok | TC-7.2, 7.3, 7.10 |
| **Mehmet Kaya** | Sınav Bekliyor | yok | TC-7.4, 7.8 |
| **Selin Arslan** | Sınav Bekliyor | yok | TC-7.5 |
| **Zeynep Şahin** | Muaf (Cambridge ile) | Cambridge C1 | tamamlanmış örnek |
| **Can Yıldız** | Reddedildi (sınav 42) | yok | tamamlanmış örnek |
| **Elif Aydın** | Muaf (sınav 72) | yok | tamamlanmış örnek |

> Tam alan değerleri (backend'e seed için) → `ydyo-test-data.md`.

## Test'lerin beklediği akış (özet)

**Detay ekranı — "Belge Değerlendirme":**
- "Belge Onay Durumu" = **dropdown** (placeholder `Seçiniz`): `Onaylandı` / `Onaylanmadı`.
- `Onaylandı` seçilince → `Sınav Sonucu` otomatik `Sınav Gerekli Değil`, `Muafiyet Sonucu` otomatik `Muaf`. İkisi de read-only.
- `Onaylanmadı` seçilince → **Sınav Puanı** girişi aktifleşir (sayısal, 0–100, adım 0.5). `Sınav Sonucu` ve `Muafiyet Sonucu` puandan otomatik türetilir (read-only, **eşik: 60**):
  - puan boş → `Sınav Sonucu Bekleniyor` / `Beklemede`
  - puan ≥ 60 → `Sınav Başarılı` / `Muaf`
  - puan < 60 → `Sınav Başarısız` / `Muaf Değil`
- `Muafiyet Sonucu` hiçbir koşulda elle değiştirilemez (read-only, sistem hesaplar).
- `Açıklama` **zorunlu**.
- Kaydet validasyonları: Belge Onay Durumu seçilmemişse `Lütfen belge onay durumunu seçiniz.`; Açıklama boşsa `Lütfen açıklama giriniz.`
- Başarılı kayıt → `Değerlendirme kaydedildi.` Kayıt **İlet'e kadar düzenlenebilir**; kilit **yalnızca** ÖİDB'ye İlet sonrası devreye girer.

**Dashboard:**
- "Tablo Oluştur" → görünen listeyi CSV olarak indirir → `Liste CSV olarak indirildi.`
- "Sonuçları ÖİDB'ye İlet": hiç `Beklemede` yoksa onay modalı (`İlet`) → `Liste başarıyla ÖİDB'ye iletildi.`, buton kilitlenir (`ÖİDB'ye İletildi`) ve kayıtlar düzenlenemez. `Beklemede` varsa bloklar: `Tüm öğrenci kayıtları tamamlanmadan liste iletilemez.`

> Not: Bu panelde **toplu CSV yükleme yoktur** — sınav sonuçları öğrenci bazında "Sınav Puanı" ile girilir; dashboard yalnızca CSV **dışa aktarır** ("Tablo Oluştur").

---

## TC-7.0 — Document Approval, Positive (Exempt Path) | High
1. YDYO dashboard'a gir → öğrenci listesi görünür.
2. **Ayşe Demir**'i seç (geçerli TOEFL belgesi, henüz değerlendirilmemiş) → detay açılır, belgeler + değerlendirme alanları görünür.
3. TOEFL belgesi linkine tıkla → PDF tarayıcıda ≤10 sn içinde açılır (plugin gerekmez).
4. "Belge Onay Durumu" = `Onaylandı` → `Sınav Sonucu` otomatik `Sınav Gerekli Değil`, `Muafiyet Sonucu` otomatik `Muaf`, ikisi read-only.
5. Açıklamaya `Cambridge C1 ile muaf` yaz → kabul, Kaydet aktif.
6. Kaydet → `Değerlendirme kaydedildi.`, listeye döner, **Ayşe Demir** `Muafiyet Sonucu` = `Muaf`.

## TC-7.1 — Save Without Açıklama, Negative (Exempt Path) | High
- **Ayşe Demir**'i aç, Belge Onay Durumu = `Onaylandı`, Açıklama boş, Kaydet → **engellenir**, hata: `Lütfen açıklama giriniz.` Kayıt yok, detayda kalır.

## TC-7.2 — Save Without Açıklama, Negative (Rejection Path) | High
- **Burak Çelik**'i aç, Belge Onay Durumu = `Onaylanmadı` (→ `Sınav Sonucu Bekleniyor`, `Muafiyet Sonucu` `Beklemede`), Sınav Puanı boş, Açıklama boş, Kaydet → **engellenir**, hata: `Lütfen açıklama giriniz.`

## TC-7.3 — Document Rejection → Exam Pending (No Score), Positive | High
1. **Burak Çelik**'i aç (İngilizce belgesi yok).
2. Belge Onay Durumu = `Onaylanmadı`, Sınav Puanı boş → `Sınav Sonucu` otomatik `Sınav Sonucu Bekleniyor`, `Muafiyet Sonucu` otomatik `Beklemede`.
3. Açıklamaya red sebebi (örn. `Geçerli dil belgesi sunulmamıştır`) → kabul.
4. Kaydet → `Değerlendirme kaydedildi.`, `Muafiyet Sonucu` = `Beklemede`, listeye döner.

## TC-7.4 — Exam Score Entry, Pass (Score ≥ 60), Positive | High
1. **Mehmet Kaya**'yı aç (Sınav Bekliyor — sınava yönlendirilmiş, `Onaylanmadı`).
2. Sınav Puanı = `72` → `Sınav Sonucu` otomatik `Sınav Başarılı`, `Muafiyet Sonucu` otomatik `Muaf`.
3. Açıklama yaz (örn. `Yeterlilik sınavını geçti`), Kaydet → `Değerlendirme kaydedildi.`, `Muafiyet Sonucu` = `Muaf`.

## TC-7.5 — Exam Score Entry, Fail (Score < 60), Positive | High
1. **Selin Arslan**'ı aç (Sınav Bekliyor).
2. Sınav Puanı = `45` → `Sınav Sonucu` otomatik `Sınav Başarısız`, `Muafiyet Sonucu` otomatik `Muaf Değil`.
3. Açıklama yaz, Kaydet → `Değerlendirme kaydedildi.`, `Muafiyet Sonucu` = `Muaf Değil`.

## TC-7.6 — Export List to CSV (Tablo Oluştur), Positive | Medium
1. Dashboard'da "Tablo Oluştur" butonuna tıkla.
2. Görünen başvuru listesi `.csv` olarak indirilir; UI: `Liste CSV olarak indirildi.`

## TC-7.7 — Transmit Final List to ÖİDB, Positive (Phase 3) | High
1. Tüm `Beklemede` kayıtları sonuçlandır (Ayşe Demir, Burak Çelik, Mehmet Kaya, Selin Arslan değerlendirilince listede `Beklemede` kalmaz) → "Sonuçları ÖİDB'ye İlet" **aktif**.
2. İlet'e tıkla → onay modalı açılır; `İlet` ile onayla → sistem `Beklemede` olmadığını doğrular ve iletimi tamamlar.
3. UI: `Liste başarıyla ÖİDB'ye iletildi.` İlet butonu kilitlenir (`ÖİDB'ye İletildi`) ve liste artık düzenlenemez.

## TC-7.8 — Incomplete List Transmission, Negative | High
1. **Mehmet Kaya** `Beklemede` (Sınav Bekliyor) iken → "Sonuçları ÖİDB'ye İlet"e tıkla.
2. Sistem bloklar, UI: `Tüm öğrenci kayıtları tamamlanmadan liste iletilemez.` ÖİDB'ye veri gitmez.

## TC-7.9 — Muafiyet Sonucu Read-Only Verification | Medium
1. **Ayşe Demir**'i aç → `Muafiyet Sonucu` görünür.
2. Doğrudan düzenlemeyi dene → alan non-interaktif (read-only), girdi kabul etmez.
3. Belge Onay Durumu'nu `Onaylandı`/`Onaylanmadı` arasında değiştir → `Muafiyet Sonucu` yalnızca sistem mantığıyla otomatik güncellenir, override edilemez.

## TC-7.10 — Auto-Derived Result from Sınav Puanı | Medium
1. **Burak Çelik**'i aç. Belge Onay Durumu = `Onaylandı` → `Sınav Sonucu` otomatik `Sınav Gerekli Değil` (puan girişi yok), `Muafiyet Sonucu` = `Muaf`.
2. `Onaylanmadı` seç → **Sınav Puanı** girişi aktifleşir; puan boşken `Sınav Sonucu` = `Sınav Sonucu Bekleniyor`, `Muafiyet Sonucu` = `Beklemede`.
3. Sınav Puanı = `72` gir → `Sınav Sonucu` otomatik `Sınav Başarılı`, `Muafiyet Sonucu` otomatik `Muaf`.
4. Sınav Puanı = `45` gir → `Sınav Sonucu` otomatik `Sınav Başarısız`, `Muafiyet Sonucu` otomatik `Muaf Değil`.

## TC-7.11 — Save Without Belge Onay Durumu, Negative | Medium
- **Ayşe Demir**'i aç, Belge Onay Durumu seçilmemiş (`Seçiniz`), Açıklama dolu olsa bile Kaydet → **engellenir**, hata: `Lütfen belge onay durumunu seçiniz.` Kayıt yok.
