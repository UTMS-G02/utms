# UTMS — API Kontratları (Frontend Referansı)

> **Versiyon:** 1.0  
> **Base URL:** `http://localhost:8080/api`  
> **Bu dosya frontend için yazılmıştır.** Backend hazır olana kadar `src/api/mock.js` dosyasındaki mock data ile çalışılır. Endpoint'ler hazırlandığında sadece `src/api/*.js` dosyalarındaki çağrılar gerçek `client` çağrılarıyla değiştirilir; sayfaların kendisi değişmez.

---

## Genel Kurallar

### Kimlik Doğrulama
`/auth/*` dışındaki tüm endpoint'ler JWT token ister:
```
Authorization: Bearer <token>
```
Token `localStorage`'da `"token"` anahtarıyla saklanır ve axios interceptor'ı her isteğe otomatik ekler.

### Standart Hata Yanıtları

| HTTP Kodu | Anlam |
|---|---|
| 400 Bad Request | Geçersiz istek gövdesi veya parametre |
| 401 Unauthorized | Token eksik veya geçersiz |
| 403 Forbidden | Yetkisiz erişim (rol uyuşmuyor) |
| 404 Not Found | Kaynak bulunamadı |
| 500 Internal Server Error | Sunucu hatası |

Hata gövdesi formatı:
```json
{ "message": "Hata açıklaması" }
```

### Sayfalama
```
GET /api/applications?page=0&size=20
```

### Roller
`STUDENT`, `OIDB`, `YDYO`, `YGK`, `DEAN`, `FACULTY_BOARD`

### Başvuru Durumları (ApplicationStatus)
```
DRAFT → SUBMITTED → OIDB_REVIEW → YDYO_REVIEW → EVALUATION_QUEUE
       → YGK_SCORED → DEAN_REVIEW → FACULTY_BOARD_REVIEW
       → FINAL_DEAN_REVIEW → RESULT_PUBLISHED → ACCEPTED / REJECTED
```
Reddedilme dalları: `OIDB_REJECTED`, `YDYO_REJECTED`, `DEAN_REJECTED`, `FACULTY_BOARD_REJECTED`

---

## STUDENT Endpoint'leri

Aşağıdaki endpoint'ler öğrenci panelinde kullanılacak olanlardır.

### POST `/auth/login`
**Yetki:** Herkese açık

**Request:**
```json
{ "email": "ali@std.iyte.edu.tr", "password": "sifre123" }
```

**Response 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "STUDENT",
  "fullName": "Ali Veli",
  "userId": 1
}
```
**Hata:** `401` — E-posta veya şifre hatalı

---

### POST `/auth/register`
**Yetki:** Herkese açık

**Request:**
```json
{
  "email": "ali@std.iyte.edu.tr",
  "password": "sifre123",
  "fullName": "Ali Veli",
  "studentNumber": "280201001",
  "department": "Bilgisayar Mühendisliği",
  "currentGpa": 3.20,
  "yksScore": 420.50,
  "yksRanking": 15000
}
```

**Response 201:**
```json
{
  "userId": 1,
  "email": "ali@std.iyte.edu.tr",
  "role": "STUDENT",
  "fullName": "Ali Veli"
}
```
**Hata:** `400` — E-posta zaten kayıtlı

---

### GET `/users/me`
**Yetki:** Tüm roller (giriş yapmış kullanıcının bilgisi)

**Response 200:**
```json
{
  "userId": 1,
  "email": "ali@std.iyte.edu.tr",
  "role": "STUDENT",
  "fullName": "Ali Veli",
  "studentNumber": "280201001",
  "department": "Bilgisayar Mühendisliği",
  "currentGpa": 3.20,
  "yksScore": 420.50,
  "yksRanking": 15000
}
```
> `studentNumber`, `department`, `currentGpa`, `yksScore`, `yksRanking` yalnızca STUDENT rolü için doludur. Diğer rollerde `null`.

---

### POST `/applications`
**Yetki:** STUDENT — yeni başvuru taslağı oluşturur

**Request:**
```json
{
  "targetDepartment": "Elektrik-Elektronik Mühendisliği",
  "targetFaculty": "Mühendislik Fakültesi"
}
```

**Response 201:**
```json
{
  "applicationId": 42,
  "status": "DRAFT",
  "createdAt": "2026-04-15T10:30:00Z"
}
```
**Hata:** `400` — Aynı bölüme bu dönemde zaten başvuru var

---

### GET `/applications`
**Yetki:** STUDENT (yalnızca kendi başvuruları)

**Query parametreleri:** `status`, `page` (default 0), `size` (default 20)

**Response 200:**
```json
{
  "content": [
    {
      "applicationId": 42,
      "targetDepartment": "Elektrik-Elektronik Mühendisliği",
      "targetFaculty": "Mühendislik Fakültesi",
      "status": "OIDB_REVIEW",
      "createdAt": "2026-04-15T10:30:00Z",
      "submittedAt": "2026-04-16T09:15:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

---

### GET `/applications/{id}`
**Yetki:** STUDENT (sahibi)

**Response 200:**
```json
{
  "applicationId": 42,
  "studentId": 1,
  "studentName": "Ali Veli",
  "targetDepartment": "Elektrik-Elektronik Mühendisliği",
  "targetFaculty": "Mühendislik Fakültesi",
  "status": "OIDB_REVIEW",
  "createdAt": "2026-04-15T10:30:00Z",
  "submittedAt": "2026-04-16T09:15:00Z",
  "documents": [
    {
      "documentId": 101,
      "docType": "TRANSCRIPT",
      "fileName": "transkript.pdf",
      "uploadedAt": "2026-04-15T11:00:00Z"
    }
  ],
  "statusHistory": [
    { "status": "DRAFT",        "changedAt": "2026-04-15T10:30:00Z" },
    { "status": "SUBMITTED",    "changedAt": "2026-04-16T09:15:00Z" },
    { "status": "OIDB_REVIEW",  "changedAt": "2026-04-16T14:00:00Z" }
  ]
}
```

---

### PATCH `/applications/{id}/submit`
**Yetki:** STUDENT (başvuru sahibi) — taslağı gönderir

**Hata:**
- `400` — Statü `DRAFT` değil
- `403` — Başvuru bu kullanıcıya ait değil

---

### POST `/applications/{id}/documents`
**Yetki:** STUDENT (sahibi)  
**Format:** `multipart/form-data`, PDF, max 10MB

**Form alanları:**
- `file`: PDF dosyası
- `docType`: aşağıdaki değerlerden biri

| docType | Açıklama |
|---|---|
| TRANSCRIPT | Transkript |
| LANGUAGE_CERT | Dil sertifikası |
| ID_CARD | Kimlik fotokopisi |
| MILITARY_STATUS | Askerlik durum belgesi |
| OTHER | Diğer |

---

### GET `/results`
**Yetki:** STUDENT (yalnızca kendi sonucu)

**Response 200:**
```json
{
  "applicationId": 42,
  "status": "ACCEPTED",
  "compositeScore": 405.32,
  "primaryListRank": 3,
  "publishedAt": "2026-05-20T15:00:00Z"
}
```

---

## Mock Data Örnekleri

`src/api/mock.js` dosyasında kullanılacak hazır veriler. Backend gelmeden önce sayfaları bu mock'lar ile çalıştırın.

```js
// src/api/mock.js

export const mockUser = {
  userId: 1,
  email: "ali@std.iyte.edu.tr",
  role: "STUDENT",
  fullName: "Ali Veli",
  studentNumber: "280201001",
  department: "Bilgisayar Mühendisliği",
  currentGpa: 3.20,
  yksScore: 420.50,
  yksRanking: 15000,
};

export const mockToken = "mock-jwt-token-for-development";

export const mockApplications = [
  {
    applicationId: 42,
    targetDepartment: "Elektrik-Elektronik Mühendisliği",
    targetFaculty: "Mühendislik Fakültesi",
    status: "OIDB_REVIEW",
    createdAt: "2026-04-15T10:30:00Z",
    submittedAt: "2026-04-16T09:15:00Z",
  },
  {
    applicationId: 43,
    targetDepartment: "Bilgisayar Mühendisliği",
    targetFaculty: "Mühendislik Fakültesi",
    status: "DRAFT",
    createdAt: "2026-04-20T14:00:00Z",
    submittedAt: null,
  },
];

export const mockApplicationDetail = {
  applicationId: 42,
  studentId: 1,
  studentName: "Ali Veli",
  targetDepartment: "Elektrik-Elektronik Mühendisliği",
  targetFaculty: "Mühendislik Fakültesi",
  status: "OIDB_REVIEW",
  createdAt: "2026-04-15T10:30:00Z",
  submittedAt: "2026-04-16T09:15:00Z",
  documents: [
    {
      documentId: 101,
      docType: "TRANSCRIPT",
      fileName: "transkript.pdf",
      uploadedAt: "2026-04-15T11:00:00Z",
    },
    {
      documentId: 102,
      docType: "ID_CARD",
      fileName: "kimlik.pdf",
      uploadedAt: "2026-04-15T11:05:00Z",
    },
  ],
  statusHistory: [
    { status: "DRAFT",       changedAt: "2026-04-15T10:30:00Z" },
    { status: "SUBMITTED",   changedAt: "2026-04-16T09:15:00Z" },
    { status: "OIDB_REVIEW", changedAt: "2026-04-16T14:00:00Z" },
  ],
};

export const mockResult = {
  applicationId: 42,
  status: "ACCEPTED",
  compositeScore: 405.32,
  primaryListRank: 3,
  publishedAt: "2026-05-20T15:00:00Z",
};

// Yardımcı: gecikme simülasyonu (loading state'leri test etmek için)
export const delay = (ms = 500) =>
  new Promise((resolve) => setTimeout(resolve, ms));
```

---

## ApplicationStatus → UI Etiket / Renk Eşlemesi

Frontend'de `StatusBadge` componenti için referans tablo:

| Status | Türkçe Etiket | Renk (antd) |
|---|---|---|
| DRAFT | Taslak | default |
| SUBMITTED | Gönderildi | blue |
| OIDB_REVIEW | ÖİDB İncelemesinde | processing |
| OIDB_REJECTED | ÖİDB Tarafından Reddedildi | red |
| YDYO_REVIEW | YDYO İncelemesinde | processing |
| YDYO_REJECTED | YDYO Tarafından Reddedildi | red |
| EVALUATION_QUEUE | Değerlendirme Sırasında | gold |
| YGK_SCORED | YGK Tarafından Puanlandı | cyan |
| DEAN_REVIEW | Dekanlık İncelemesinde | processing |
| DEAN_REJECTED | Dekanlık Tarafından Reddedildi | red |
| FACULTY_BOARD_REVIEW | Fakülte Kurulu İncelemesinde | processing |
| FACULTY_BOARD_REJECTED | Fakülte Kurulu Reddetti | red |
| FINAL_DEAN_REVIEW | Nihai Dekanlık Onayında | processing |
| RESULT_PUBLISHED | Sonuç Yayınlandı | green |
| ACCEPTED | Kabul Edildi | green |
| REJECTED | Reddedildi | red |
