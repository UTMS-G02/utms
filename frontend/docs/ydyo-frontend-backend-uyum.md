# YDYO (UC-7) — Frontend ↔ Backend Uyum Raporu

> Amaç: YDYO başvuru değerlendirme ekranını (`src/api/ydyo.js` + `YdyoDashboard` / `YdyoApplicationDetail`) gerçek backend'e bağlamadan önce farkları netleştirmek.
> Durum: **Endpoint iskeleti uyumlu, ama `ApplicationResponse` UI'nin ihtiyaç duyduğu alanların çoğunu döndürmüyor.** Tam bağlama backend değişikliği gerektirir.
>
> Şu an frontend tamamen **mock** (`ydyo.js` içindeki in-memory liste) ile çalışıyor ve TC-7.x test case'leri buna göre geçiyor.

İlgili backend kaynak dosyaları:
- `backend/.../api/application/dto/ApplicationResponse.java`
- `backend/.../api/application/dto/YdyoReviewRequest.java`
- `backend/.../api/application/dto/YdyoExamResultRequest.java`
- `backend/.../dal/application/entity/ApplicationStatus.java`
- `ApplicationService.toResponse()` (~satır 407-419) ve `getAllApplications(...)`

---

## 1. Endpoint eşlemesi (iskelet uyumlu ✅)

Base URL: backend `http://localhost:8080`, tüm yollar `/api` altında. Frontend `client.js` `baseURL: '/api'` (dev proxy ile 8080'e gider).

| Frontend `ydyoApi` | Backend | Durum |
|---|---|---|
| `getApplications(status)` → `.content` | `GET /api/applications?status=&page=&size=` → Spring `Page` | ✅ Şekil uyumlu (`.content`, `.totalElements`...) |
| `getApplicationById(id)` | `GET /api/applications/{id}` | ✅ |
| `getDocuments(applicationId)` | `GET /api/applications/{applicationId}/documents` | ✅ (alan adları farklı, bkz. §3) |
| `getDocumentDownloadUrl(documentId)` | `GET /api/documents/{documentId}/download` | ✅ |
| `submitInitialReview(id, ...)` | `PATCH /api/applications/{id}/ydyo-initial-review` | ⚠️ İstek alan adı farkı (bkz. §2) |
| `submitExamResult(id, ...)` | `PATCH /api/applications/{id}/ydyo-exam-result` | ✅ Gövde birebir |

**Liste filtresi:** Backend tek `status` parametresi alıyor; board ise tüm `YDYO_*` aşamalarını birden gösteriyor. Çözüm: ya her status için ayrı çağrı yapıp birleştir, ya filtresiz çek + client-side `YDYO_*` filtrele. (Mock'ta da bu not düşülü.)

**Yetki:** `ydyo-initial-review` ve `ydyo-exam-result` → `hasAnyRole('YDYO','ROLE_YDYO')`. Liste/detay → STUDENT/OIDB/YDYO (+FACULTY/DEAN detayda).

---

## 2. İstek (request) gövdesi farkları

### `ydyo-exam-result` — ✅ uyumlu
Frontend gönderiyor: `{ examScore, passed, notes, reviewerId }`
Backend `YdyoExamResultRequest`: `examScore (Double)`, `passed (Boolean)`, `notes`, `reviewerId (Integer)`, `reviewer (Staff, opsiyonel)`.
→ Birebir uyumlu. (Frontend `examScore`'u artık puan tabanlı gönderiyor; uyumlu.)

### `ydyo-initial-review` — ⚠️ alan adı doğrulanmalı
Frontend gönderiyor: `{ approved, requiresExam, notes, reviewerId }`
Backend `YdyoReviewRequest` alanı: **`isApproved` (boolean)**, `requiresExam (Boolean)`, `notes`, `reviewerId (Long)`, `reviewer (Staff)`.
→ Jackson, `isApproved` boolean getter'ını çoğunlukla JSON `approved` olarak map eder; bu durumda frontend'in `approved` göndermesi **çalışabilir**. Ama **doğrulanmalı** (Lombok `@Data`/`@JsonProperty` durumuna bağlı). Güvenli yol: backend'de `@JsonProperty("approved")` netleştirmek veya frontend'i `isApproved`'a uydurmak.

### `reviewerId` kaynağı
Backend şu an `reviewerId`'yi **request gövdesinden** okuyor (JWT principal'dan değil). Frontend zaten `reviewerId` gönderiyor → uyumlu. (Backend yorumları "ileride JWT'den alınmalı" diyor; o zaman frontend gövdeden çıkarabilir.)

---

## 3. Yanıt (response) alan boşlukları — ASIL ENGEL ❌

Frontend'in türetme fonksiyonları (`deriveDocumentApproval`, `deriveExamStatus`, `deriveExemptionStatus`) ve detay ekranı şu alanlara dayanıyor: `status`, `exemptionApproved`, `requiresExam`, `examPassed`, `examScore`, ayrıca kimlik/iletişim alanları.

Backend `ApplicationResponse` (ve `toResponse()`'un fiilen doldurduğu) bunların çoğunu **vermiyor**.

| UI'nin beklediği alan | Backend karşılığı | Durum |
|---|---|---|
| `studentName` | yok (Student entity'de var) | ❌ DTO'ya eklenmeli |
| `tcKimlikNo` | yok (Student entity'de var) | ❌ |
| `email` | yok | ❌ |
| `phone` | yok | ❌ |
| `currentUniversity` | `currentUniversity` | ✅ |
| `currentDepartment` | `currentDepartment` | ✅ |
| `targetDepartment` | `targetDepartment` | ✅ |
| `targetFaculty` | `targetFaculty` (DTO'da var ama `toResponse` set etmiyor) | ⚠️ |
| `academicYear` | `academicYear` | ✅ |
| `status` | `status` | ✅ |
| `exemptionApproved` | `ydyoApproved` | ❌ ad farkı + `toResponse` set etmiyor |
| `requiresExam` | yok (sadece request'te) | ❌ response'a eklenmeli |
| `examScore` | entity `ydyoExamScore` | ❌ response'a eklenmeli |
| `examPassed` | yok | ❌ (ya alan, ya `passed` türetimi) |
| `submittedAt` | entity `submissionDate` | ❌ response'a eklenmeli |
| `notes` | `ydyoNotes` (+`oidbNotes`) | ⚠️ ad farkı + `toResponse` set etmiyor |
| `englishCertificate` | backend'de kavram yok | ❌ ayrı modelleme gerekir |

> **Kritik:** `toResponse()` şu an DTO'da tanımlı `ydyoApproved/ydyoNotes/ydyoReviewedBy/...` alanlarını **bile** doldurmuyor. Fiilen dönen alanlar: `id, status, academicYear, targetDepartment, currentUniversity, currentFaculty, currentDepartment, gpa`.

### Belgeler
`getDocuments` Map döndürüyor: `documentId, applicationId, documentType, fileName, filePath, ydyoApproved, documentUploadDate, active`.
Frontend belge modeli: `documentId, docType, fileName`.
→ `documentType` ≠ `docType` (mapper'da çevrilmeli). `englishCertificate` backend'de ayrı bir kavram değil — muhtemelen `documentType == LANGUAGE_CERT` olan belge olarak türetilmeli.

---

## 4. Durum (status) enum'u — uyumlu ✅

Frontend `YDYO_STATUS`: `YDYO_REVIEW, YDYO_EXAM_PENDING, YDYO_ACCEPTED, YDYO_REJECTED` — hepsi backend `ApplicationStatus`'ta mevcut. Backend ek aşamalar da içeriyor (DRAFT, OIDB_*, DEAN_*, YGK_*, FACULTY_BOARD_*, APPROVED, REJECTED) ama YDYO board'u sadece `YDYO_*` ile ilgileniyor.

Geçiş mantığı da uyumlu:
- initial-review: `YDYO_REVIEW` → `YDYO_EXAM_PENDING` (requiresExam) / `YDYO_ACCEPTED` (approved)
- exam-result: `YDYO_EXAM_PENDING` → `YDYO_ACCEPTED` (passed) / `YDYO_REJECTED` (!passed)

---

## 5. Yapılacaklar

### Backend (bağlamanın ön koşulu)
- [ ] `ApplicationResponse`'a ekle: `studentName, tcKimlikNo, email, phone` (Student entity'den), `requiresExam`, `examScore` (`ydyoExamScore`), `examPassed`/`passed`, `submittedAt` (`submissionDate`).
- [ ] `exemptionApproved` semantiğini netleştir: `ydyoApproved` alanını response'a koy ya da UI'nin beklediği adla map et.
- [ ] `notes` için `ydyoNotes`'u response'a koy (ya da UI `ydyoNotes` okusun).
- [ ] `toResponse()`'u bu alanları **fiilen set edecek** şekilde güncelle (şu an çoğu boş dönüyor).
- [ ] `ydyo-initial-review` için `approved` ↔ `isApproved` JSON adını doğrula/sabitле (`@JsonProperty`).
- [ ] (Opsiyonel) `englishCertificate`: `LANGUAGE_CERT` belgesini ayrı alan olarak veya türetilebilir şekilde sun.

### Frontend (backend hazır olunca)
- [ ] `ydyo.js`'teki her TODO'da yazılı gerçek `apiClient` çağrısını aç.
- [ ] backend→UI **alan eşleme (mapper)** katmanı: `ydyoApproved→exemptionApproved`, `ydyoNotes→notes`, `submissionDate→submittedAt`, `documentType→docType`, `LANGUAGE_CERT→englishCertificate`.
- [ ] Board için çok-status çekme stratejisi (filtresiz çek + client-side `YDYO_*` filtre, ya da status başına çağrı + merge).
- [ ] `VITE_USE_MOCK` gibi bir bayrakla mock↔real geçişi (testler/demo mock'ta kalsın, backend hazır olunca aç).
- [ ] `reviewerId` kaynağı (gövde mi JWT mi) backend kararına göre.

---

## 5.1 — Yapıldı (bu commit) ✅

**Backend** (`ApplicationResponse` + `ApplicationService.toResponse()`):
- `ApplicationResponse`'a eklendi: `studentName, tckn, email, phoneNumber, submissionDate, requiresExam, examScore, examPassed` ve hafif `documents` listesi (`DocumentSummary{documentId, documentType, fileName}`).
- `toResponse()` artık tüm alanları **fiilen** dolduruyor (öğrenci kimliği, ÖİDB/YDYO inceleme alanları, belgeler) ve şu türetimleri yapıyor:
  - `requiresExam = ydyoApproved == null ? null : !ydyoApproved`
  - `examPassed`: `YDYO_REJECTED → false`, belge onaysız `YDYO_ACCEPTED → true`, diğer → `null`
- `getAllApplications` ve `getApplicationById` → `@Transactional(readOnly = true)` (lazy `documents` erişimi güvenli).

**Frontend** (`src/api/ydyo.js`):
- `mockApi` / `realApi` ayrımı + `mapApplication()` eşleyici (`id→applicationId`, `ydyoApproved→exemptionApproved`, `ydyoNotes→notes`, `tckn→tcKimlikNo`, `phoneNumber→phone`, `submissionDate→submittedAt`, `documentType→docType`, dil belgesinden `englishCertificate` türetimi).
- `VITE_USE_MOCK` bayrağı — **varsayılan mock** (testler/demo korunur). Gerçek backend için `.env`'de `VITE_USE_MOCK=false`.
- Board için: filtresiz çekip client-side `YDYO_*` filtreleme.

Doğrulama: backend `./mvnw -o clean compile` → **BUILD SUCCESS**; frontend `eslint src/api/ydyo.js` → temiz.

## 5.1.1 — Canlı doğrulama (native Postgres, Docker'sız) ✅

`./mvnw spring-boot:run` + `localhost:5432/utms_db` ile uçtan uca denendi (YDYO kullanıcısı `ydyo@iyte.edu.tr` / `test123`, DataInitializer seed'i):

- **Login** `POST /api/auth/login` → JWT alındı (LoginResponse `{userId, firstName, lastName, role, token}`).
- **GET** `/api/applications?status=YDYO_REVIEW` ve filtresiz → yeni alanların hepsi **dolu** geldi: `studentName, tckn, email, phoneNumber, submissionDate, requiresExam, examScore, examPassed, documents[]`.
- **Türetim doğru:** YDYO_ACCEPTED + `ydyoApproved=false` + `ydyoExamScore=72` kaydı → `requiresExam=true`, `examPassed=true` (frontend derive: Onaylanmadı → Sınav Başarılı → Muaf).
- **PATCH** `ydyo-initial-review` `{approved:false,...}` → `YDYO_EXAM_PENDING`, `ydyoApproved=false`. ⇒ **`approved` → `isApproved` Jackson eşleşmesi çalışıyor** (aşağıdaki #1 çözüldü).
- **PATCH** `ydyo-exam-result` `{examScore:65, passed:true}` → `YDYO_ACCEPTED`, `examScore=65`, `examPassed=true`.

## 5.2 — Kalan bilinen kısıtlar (real modda) ⚠️

1. ~~**`isApproved` ↔ `approved`:**~~ ✅ **Çözüldü/doğrulandı** — canlı PATCH ile `approved` JSON alanı backend `isApproved`'a doğru bağlandı (§5.1.1).
2. **Tek adımda sınav puanı:** Frontend detay ekranı "Onaylanmadı + puan" girince doğrudan `ydyo-exam-result` çağırır. Backend bu endpoint'i yalnızca statü `YDYO_EXAM_PENDING` iken kabul eder. Kayıt henüz `YDYO_REVIEW` ise önce `ydyo-initial-review` (requiresExam=true) ile `EXAM_PENDING`'e taşınmalı. → Real modda `realApi.submitExamResult` iki adımı zincirleyecek şekilde güçlendirilmeli **ya da** akış iki kayda bölünmeli.
3. **Tekrar düzenleme:** Frontend "İlet'e kadar düzenlenebilir" varsayar; backend `processYdyoReview` yalnızca `YDYO_REVIEW` statüsünde çalışır. Karar verilmiş kaydı yeniden düzenleme real modda reddedilir.
4. **`reviewerId` yok sayılıyor:** Backend `req.getReviewer()` (Staff) okuyor, frontend `reviewerId` (sayı) gönderiyor → şu an `ydyoReviewedBy` null kalır. Backend reviewerId'yi id'den çözmeli ya da JWT'den almalı.
5. **`submissionDate` boş:** `submit()` bu alanı set etmiyor (mevcut backend davranışı) → `submittedAt` çoğu kayıtta boş gelir (`—`).

## 6. Özet
- **Endpoint'ler ve status enum'u uyumlu.** `submitExamResult` gövdesi birebir.
- **Çözüldü:** `ApplicationResponse` artık öğrenci kimliği + YDYO durum/puan/tarih/belge alanlarını döndürüyor ve `toResponse()` hepsini dolduruyor (§5.1). Frontend'te mapper + `VITE_USE_MOCK` bayrağı eklendi; varsayılan mock olduğu için TC-7.x korunuyor.
- **Sıradaki:** Real modu uçtan uca çalıştırmak için §5.2'deki kısıtlar (özellikle 1 = `approved` JSON adı, 2 = tek-adım sınav puanı akışının iki PATCH'e bölünmesi) giderilmeli. Çalışan backend + DB ile `VITE_USE_MOCK=false` ile denenmeli.
