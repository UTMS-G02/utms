# UC-7 — YDYO (Manage English Proficiency Assessment) Test Case'leri

> Bu, graders'ın YDYO panelini test edeceği case'lerin tam listesi. Yazılım bunları geçecek şekilde olmalı. Tüm Türkçe mesajlar **birebir** eşleşmeli.

## Test'lerin beklediği akış (özet)

**Detay ekranı — "Belge Değerlendirme":**
- "Belge Onay Durumu" = **dropdown**: `Onaylandı` / `Onaylanmadı`.
- `Onaylandı` seçilince → `Sınav Sonucu` otomatik `Sınav Gerekli Değil`, `Muafiyet Sonucu` otomatik `Muaf`. İkisi de read-only.
- `Onaylanmadı` seçilince → `Sınav Sonucu` dropdown aktifleşir (`Sınav Sonucu Bekleniyor` / `Sınav Başarılı` / `Sınav Başarısız`), `Muafiyet Sonucu` `Beklemede`.
- `Sınav Başarılı` → `Muafiyet Sonucu` = `Muaf`; `Sınav Başarısız` → `Muaf Değil` (otomatik).
- `Muafiyet Sonucu` hiçbir koşulda elle değiştirilemez (read-only, sistem hesaplar).
- `Açıklama` **zorunlu**; boşsa kaydetme.
- Kayıt **İlet'e kadar düzenlenebilir**: karar (`Onaylandı`/`Onaylanmadı`) değiştirilebilir, `Açıklama` not gibi güncellenebilir. Kilit **yalnızca** ÖİDB'ye İlet sonrası devreye girer.

**Toplu sınav — CSV yükleme** ("Toplu Sınav Sonucu Yükle"):
- CSV kolonları: `studentId, score, result` (result = `Sınav Başarılı` / `Sınav Başarısız`).
- İşlenince özet: toplam / başarılı / hatalı sayısı.

**İlet** ("Sonuçları ÖİDB'ye İlet"): tüm kayıtlar finalize ise iletir, `Beklemede` varsa bloklar.

---

## TC-7.0 — Document Approval, Positive (Exempt Path) | High
1. YDYO dashboard'a gir → öğrenci listesi görünür.
2. Geçerli TOEFL/Cambridge belgesi olan öğrenciyi seç → detay açılır, belgeler + değerlendirme alanları görünür.
3. Belge linkine tıkla → PDF tarayıcıda ≤10 sn içinde açılır (plugin gerekmez).
4. "Belge Onay Durumu" = `Onaylandı` → `Sınav Sonucu` otomatik `Sınav Gerekli Değil`, `Muafiyet Sonucu` otomatik `Muaf`, ikisi read-only.
5. Açıklamaya `Cambridge C1 ile muaf` yaz → kabul, Kaydet aktif.
6. Kaydet → kayıt saklanır, listeye döner, öğrenci `Muafiyet Sonucu` = `Muaf`.

## TC-7.1 — Save Without Açıklama, Negative | High
- Belge Onay Durumu = `Onaylandı`, Açıklama boş, Kaydet → **engellenir**, hata: `Lütfen açıklama giriniz.` Kayıt yok, detayda kalır.

## TC-7.2 — Save Without Açıklama, Negative (Rejection Path) | High
- Belge Onay Durumu = `Onaylanmadı` (→ `Sınav Sonucu Bekleniyor`, `Muafiyet Sonucu` `Beklemede`), Açıklama boş, Kaydet → **engellenir**, hata: `Lütfen açıklama giriniz.`

## TC-7.3 — Document Rejection, Positive (Exam Path) | High
1. Geçersiz/eksik belgeli öğrenciyi aç.
2. Belge Onay Durumu = `Onaylanmadı` → `Sınav Sonucu` otomatik `Sınav Sonucu Bekleniyor`, `Muafiyet Sonucu` otomatik `Beklemede`.
3. Açıklamaya red sebebi (örn. `Geçerli dil belgesi sunulmamıştır`) → kabul.
4. Kaydet → `Muafiyet Sonucu` = `Beklemede`, listeye döner.

## TC-7.4 — Bulk Exam Result Upload, Positive (All Pass) | High
1. "Toplu Sınav Sonucu Yükle" → yükleme alanı görünür.
2. CSV: `studentId, score, result`, hepsi `Sınav Başarılı`.
3. Yükle → sistem kabul eder, ilerleme göstergesi.
4. Her satır işlenir → `Sınav Başarılı`, `Muafiyet Sonucu` = `Muaf`.
5. Özet: toplam işlenen, başarılı sayısı, 0 hata. Önceden `Beklemede` olanlar artık `Muaf`.

## TC-7.5 — Bulk Exam Result Upload, Mixed (Pass & Fail) | High
- Karışık CSV → geçenler: `Sınav Başarılı` + `Muaf`; kalanlar: `Sınav Başarısız` + `Muaf Değil`. Özet Muaf / Muaf Değil sayılarını doğru gösterir.

## TC-7.6 — Bulk Upload, Negative (Invalid File & Content) | Medium
- Non-CSV (örn. .xlsx) → `Geçersiz dosya formatı. Lütfen bir .csv dosyası yükleyiniz.`
- Eksik kolon (örn. `score` yok) → `Geçersiz dosya yapısı. Lütfen zorunlu sütunların (studentId, score, result) mevcut olduğunu kontrol ediniz.` Kayıt güncellenmez.
- Tanınmayan studentId'ler → satır atlanır, özette "hatalı" olarak raporlanır, o öğrenciler güncellenmez.
- Boş satırlar → atlanır, özet atlanan sayıyı yansıtır.

## TC-7.7 — Transmit Final List to ÖİDB, Positive (Phase 3) | High
1. Dashboard'da hiç `Beklemede` yok → "Sonuçları ÖİDB'ye İlet" **aktif**.
2. İlet'e tıkla → sistem `Beklemede` olmadığını doğrular ve iletimi tamamlar.
3. UI: `Liste başarıyla ÖİDB'ye iletildi.` İlet butonu kilitlenir ("ÖİDB'ye İletildi") ve liste artık düzenlenemez (iletildi olarak işaretlenir).

## TC-7.8 — Incomplete List Transmission, Negative | High
1. En az bir öğrenci `Beklemede` → İlet butonu disabled ya da tıklayınca bloklar.
2. İlet → sistem bloklar, UI: `Tüm öğrenci kayıtları tamamlanmadan liste iletilemez.` ÖİDB'ye veri gitmez.

## TC-7.9 — Muafiyet Sonucu Read-Only Verification | Medium
1. Herhangi bir öğrenciyi aç → `Muafiyet Sonucu` görünür.
2. Doğrudan düzenlemeyi dene → alan non-interaktif (greyed/read-only), girdi kabul etmez.
3. Belge Onay Durumu'nu `Onaylandı`/`Onaylanmadı` arasında değiştir → `Muafiyet Sonucu` yalnızca sistem mantığıyla otomatik güncellenir, override edilemez.

## TC-7.10 — Dynamic Sınav Sonucu Dropdown Filtering | Medium
1. Belge Onay Durumu = `Onaylandı` → `Sınav Sonucu` otomatik `Sınav Gerekli Değil`, sınav seçenekleri seçilemez, `Muafiyet Sonucu` = `Muaf`.
2. `Onaylanmadı` → `Sınav Sonucu` dropdown aktif: `Sınav Sonucu Bekleniyor`, `Sınav Başarılı`, `Sınav Başarısız`.
3. `Sınav Başarılı` seç → `Muafiyet Sonucu` otomatik `Muaf`.
4. `Sınav Başarısız` seç → `Muafiyet Sonucu` otomatik `Muaf Değil`.
