# UTMS — YDYO (Yabancı Diller) Personel Paneli Kurulumu

## Görev
UTMS projesine YDYO personel panelini **sıfırdan** kur. `src/pages/ydyo/` klasörü henüz yok, onu oluştur. Panel, mevcut **student panelinin** kod stilini ve mimarisini birebir takip etmeli.

## ÖNCE YAP: Mevcut paternleri incele
Kod yazmadan önce şu dosyaları oku ve paternlerini birebir kopyala:
- `src/pages/student/` (Dashboard, ApplicationList, ApplicationDetail, Profile, ApplicationForm) → inline `styles` objesi kullanımı, DM Sans font, `#8B1A2B` accent rengi, badge/kart yapısı.
- `src/api/client.js` (axios + JWT interceptor) ve `src/api/applications.js` (çağrı paterni).
- `src/api/mock.js` → mock data + TODO marker paterni (önce mock, sonra gerçek API).
- `src/components/Layout/AppLayout.jsx` → role-based menü.
- `src/App.jsx` → role-protected route paterni (`/student/*`).

Tutarlılık şart: yeni dosyalar mevcutlardan ayırt edilemez stilde olmalı.

## Stack & Kurallar
- React 19 + Vite 8 + Ant Design 6 + Axios + React Router 7.
- Vite proxy `/api` → `localhost:8080`. localStorage token key: `utms_token`.
- **Kod ve yorumlar İngilizce** (proje design constraint'i). **UI metni Türkçe.**
- Stil: AntD üstüne mevcut paneldeki gibi inline `styles` objesi + DM Sans + accent `#8B1A2B`.
- **Önce MOCK data ile çalış**, gerçek API çağrılarını `// TODO: replace with real API` marker'ıyla bırak (student panelindeki gibi).

---

## API Kontratı (Swagger'dan doğrulandı)
Tüm istekler header: `Authorization: Bearer <token>` (mevcut client.js interceptor zaten ekliyor).

| Aksiyon | Method + Path |
|---|---|
| YDYO başvuru listesi | `GET /api/applications?status=YDYO_REVIEW` (Spring `Page`, liste `.content` içinde) |
| Başvuru detayı | `GET /api/applications/{id}` (ApplicationResponse) |
| Başvuru belgeleri | `GET /api/applications/{applicationId}/documents` |
| Belge indir | `GET /api/documents/{documentId}/download` |
| Aşama 1 — Evrak inceleme | `PATCH /api/applications/{id}/ydyo-initial-review` |
| Aşama 2 — Sınav sonucu | `PATCH /api/applications/{id}/ydyo-exam-result` |
| Belge onayı (YDYO) | `PATCH /api/documents/{documentId}/ydyo-approval?approved=true` |

> `ydyo-review` endpoint'i VAR ama `ydyo-initial-review` ile aynı body'ye sahip eski/legacy versiyon — **kullanma**, granular ikiliyi kullan.

### Request body'leri — SADECE şunları gönder
**Aşama 1 (`ydyo-initial-review`):**
```json
{ "approved": true, "requiresExam": false, "notes": "...", "reviewerId": 12 }
```
**Aşama 2 (`ydyo-exam-result`):**
```json
{ "examScore": 78.5, "passed": true, "notes": "...", "reviewerId": 12 }
```

> ⚠️ Swagger şemasında body içinde tam bir `reviewer` objesi (passwordHash, departmentId vb.) görünüyor — bu entity yansıması, **GÖNDERME**. Sadece `reviewerId` yolla. passwordHash içeren objeyi göndermek güvenlik açığı.

- `reviewerId` = giriş yapan YDYO personelinin id'si. Mevcut auth context'ten / `GET /api/auth/me`'den al — student panelindeki current-user erişim paternini kullan.
- `notes` **zorunlu**: boşsa kaydetme, "Lütfen açıklama giriniz." uyarısı göster (SRS UC-7, 8-EX).
- `examScore` ondalıklı olabilir (decimal number input).

### Aşama 1 — 3 sonuç mapping'i (iki bool → üç aksiyon)
| UI butonu | Body |
|---|---|
| **Muaf Say** | `{ approved: true,  requiresExam: false }` |
| **Sınava Girecek** | `{ approved: false, requiresExam: true }` |
| **Reddet** | `{ approved: false, requiresExam: false }` |

> Mock'ta bu mapping'i kur. Gerçek API'ye bağlarken `approved`'ın backend'deki anlamı doğrulanacak; farklıysa mapping tek noktadan değişir. `// TODO: verify approved/requiresExam semantics on real binding` notu bırak.

### Aşama 2 — sınav sonucu
- `passed`'i sistem HESAPLAMAZ (backend'de eşik yok). Personel **Geçti/Kaldı'yı elle** işaretler; `examScore` ayrıca kaydedilir.

---

## Status akışı (state machine)
```
YDYO_REVIEW
   ├─ Muaf Say        → YDYO_ACCEPTED
   ├─ Sınava Girecek  → YDYO_EXAM_PENDING ──(exam-result)──┬─ Geçti → YDYO_ACCEPTED
   └─ Reddet          → YDYO_REJECTED                       └─ Kaldı → YDYO_REJECTED
```
Belge onayı (`ydyo-approval`) ayrı bir aksiyon, her belge dosyası için bağımsız.

---

## Oluşturulacak dosyalar
```
src/api/ydyo.js                 # YDYO API çağrıları + mock (applications.js paterni)
src/pages/ydyo/
  YdyoDashboard.jsx             # Ekran 1 — board (filtre + liste + aksiyonlar)
  YdyoApplicationDetail.jsx     # Ekran 2 — detay (inline expand veya ayrı sayfa)
  YdyoExamResults.jsx           # Ekran 3 — toplu sınav giriş tablosu
```
Ayrıca güncelle:
- `src/components/Layout/AppLayout.jsx` → YDYO rolü için menü (mevcut role-menü paternine uygun).
- `src/App.jsx` → `/ydyo/*` route'ları, mevcut role-guard paterniyle, YDYO rolü için korumalı.

> Role guard'ı `App.jsx`'teki mevcut paterne göre ekle; YDYO rol string'ini koddaki/`auth/me`'deki gerçek değere göre kullan (repo'dan tespit et).

---

## Ekranlar

### Ekran 1 — YdyoDashboard (board)
Header: kurum logosu + "Yabancı Diller Yüksekokulu" / "Yatay Geçiş Başvuru Yönetim Sistemi", sağ üstte "Çıkış Yap".

**Filtre çubuğu:** Akademik Yıl (select), Sınav Durumu (select), Muafiyet Durumu (select), sağda "Başvuru: N" sayacı.

**Aksiyon satırı (sağ):**
- **"Tablo Oluştur"** → görünen listeyi CSV olarak indir (client-side export). Backend çağrısı yok.
- **"Sonuçları ÖİDB'ye İlet"** (primary, accent) → backend'de forward endpoint'i YOK. Bu buton sadece **client-side guard**: listede `YDYO_REVIEW` veya `YDYO_EXAM_PENDING` kalmış mı kontrol et. Kalmışsa "Tüm öğrenci kayıtları tamamlanmadan liste iletilemez." uyarısı (SRS 14-EX). Hepsi finalize ise "Sonuçlar iletildi" onayı. (ÖİDB zaten `YDYO_ACCEPTED`/`YDYO_REJECTED` olanları kendi listesinde çekiyor.)

**Liste:** her başvuru bir kart/satır. Kolonlar:
- Ad Soyad, E-posta, Telefon
- **Belge Onayı** badge: `Beklemede` / `Onaylandı` / `Onaylanmadı`
- **Sınav Sonucu** badge: `Henüz Belirlenmedi` / `Sınav Gerekli Değil` / `Sınav Sonucu Bekleniyor` / `Sınav Başarılı` / `Sınav Başarısız`
- **Muafiyet Sonucu** badge: `Beklemede` / `Muaf` / `Muaf Değil`
- Açıklama (notes)
- Sağda chevron → detayı aç (accordion expand, prototipteki gibi).

**Badge renkleri (soft tint):**
- Beklemede / bekleniyor → amber (`#FEF3C7` bg, `#B45309` text)
- Onaylandı / Muaf / Başarılı → green (`#D1FAE5` bg, `#047857` text)
- Onaylanmadı / Muaf Değil / Başarısız → red (`#FEE2E2` bg, `#B91C1C` text)
- Henüz Belirlenmedi / Gerekli Değil → gray (`#F3F4F6` bg, `#6B7280` text)

Üstte ayrıca **"Toplu Sınav Sonucu Gir"** butonu → Ekran 3'e yönlendir.

### Ekran 2 — YdyoApplicationDetail
Prototipte satır accordion ile açılıyor; aynı şekilde inline expand yap (ya da student panelinde ayrı sayfa varsa onu mirror'la — tutarlı olanı seç).

**Öğrenci Detay Bilgileri:** Ad Soyad, TC Kimlik No, E-posta, Telefon, Mevcut Üniversite, Mevcut Bölüm, Hedef Bölüm, Başvuru Tarihi, Akademik Yıl + üç durum badge'i + Açıklama (hepsi read-only).

**Başvuru Belgeleri** paneli: belge listesi (örn. Transkript.pdf, Onay_Belgesi.pdf). Her belge linki açılabilir/indirilebilir (`/api/documents/{id}/download`) + her belge için **Onayla / Reddet** kontrolü → `PATCH /api/documents/{documentId}/ydyo-approval?approved=true|false`.

**İngilizce Yeterlilik Belgesi** paneli: belge varsa link, yoksa "Belge yüklenmemiş".

**Muafiyet Değerlendirme** (Aşama 1 kontrolü):
- Prototipteki dropdown formunu **3 buton** ile değiştir: **Muaf Say** / **Sınava Girecek** / **Reddet** (yukarıdaki body mapping'iyle).
- Zorunlu **Açıklama** textarea (boşsa kaydetme).
- Sınav sonucu burada GİRİLMEZ (toplu ekranda). Öğrenci `YDYO_EXAM_PENDING` ya da finalize ise, sınav sonucu/muafiyet durumunu **read-only** göster.

### Ekran 3 — YdyoExamResults (toplu sınav)
- `GET /api/applications?status=YDYO_EXAM_PENDING` ile sınava girecekleri çek, tablo halinde listele.
- Her satır: Ad Soyad | **Sınav Notu** (decimal number input) | **Sonuç** (Geçti/Kaldı toggle veya segmented) | **Açıklama** (text input, zorunlu).
- "Kaydet" → doldurulmuş her satır için ayrı ayrı `PATCH /api/applications/{id}/ydyo-exam-result` (client-side loop; `Promise.allSettled` ile, kısmi başarıyı handle et).
- Bitince **özet** göster: "N kayıt: X geçti, Y kaldı, Z hata." (SRS upload-summary mantığı.)
- Açıklaması boş / notu girilmemiş satırları kaydetme, inline uyar.

---

## Mock veri notu
`src/api/ydyo.js` içinde mock applications dizisi tut; karışık statülerde örnek kayıtlar olsun (REVIEW, EXAM_PENDING, ACCEPTED, REJECTED) ki board tüm badge'leri render etsin. Mock fonksiyonları gerçek imzalarla eşleşsin (params, Page-benzeri `{ content: [...] }` dönüşü). Gerçek alan adları `ApplicationResponse` şemasıyla bağlanırken netleşecek → `// TODO: map to real ApplicationResponse fields` bırak.

> Not: liste endpoint'i tek statü çekiyor (`?status=...`). Board'un tüm YDYO aşamalarını göstermesi için gerçek bağlamada ya statü başına çağrı + merge, ya filtresiz çekip client'ta `YDYO_*` süzme gerekecek → `// TODO` bırak. Mock'ta tek çağrı hepsini döndürsün.

## Çıktı
Adım adım ilerle: önce `src/api/ydyo.js` (mock + imzalar), sonra Ekran 1, 2, 3, en son `App.jsx` + `AppLayout.jsx` entegrasyonu. Her adımda mevcut student panel stiliyle tutarlılığı koru.
