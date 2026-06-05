# UC-7 / YDYO — Test Öğrencileri (Database Seed Verisi)

> YDYO panelinin UC-7 test case'lerinde **isimle** referans verilen 7 sabit öğrenci.
> Bu kayıtlar **gerçek database'e** eklendiğinde, backend çalıştığında YDYO paneli bu
> öğrencileri listeler ve TC-7.0 … TC-7.11 isimleriyle çalışır.
>
> ⚠️ Test sırasında bu öğrencilerin durumu **değişir** (değerlendirme/iletim). Teslimden
> önce orijinal hâline geri döndürülmelidir.

## Ortak değerler

- **Student (User):** `role = STUDENT`, `active = true`, parola `test123` (bcrypt hash'lenecek), `kvkkAcceptedAt = now`.
- **Application:** `academicYear = 2025-2026`, `semester = 3`, `targetFaculty = Mühendislik Fakültesi`, `currentFaculty = Mühendislik Fakültesi`, current bölüm = target bölüm.
- ⚠️ DB ekibi başvuruda **yalnızca** şu YDYO alanlarını set etsin: `status`, `ydyoApproved`, `ydyoExamScore`, `ydyoNotes`.
  (`Sınav Sonucu`, `Muafiyet Sonucu`, `requiresExam` panelde bunlardan **otomatik türetilir** — kolon olarak girilmez.)

## 1) Öğrenciler (users / students)

| firstName | lastName | email | tckn | phoneNumber | dateOfBirth |
|---|---|---|---|---|---|
| Ayşe | Demir | ayse.demir@std.iyte.edu.tr | 12345678901 | 5321112233 | 2003-03-12 |
| Burak | Çelik | burak.celik@std.iyte.edu.tr | 67890123456 | 5376667788 | 2002-09-09 |
| Mehmet | Kaya | mehmet.kaya@std.iyte.edu.tr | 23456789012 | 5332223344 | 2002-07-21 |
| Selin | Arslan | selin.arslan@std.iyte.edu.tr | 78901234567 | 5387778899 | 2003-08-22 |
| Zeynep | Şahin | zeynep.sahin@std.iyte.edu.tr | 34567890123 | 5343334455 | 2003-01-05 |
| Can | Yıldız | can.yildiz@std.iyte.edu.tr | 45678901234 | 5354445566 | 2002-11-30 |
| Elif | Aydın | elif.aydin@std.iyte.edu.tr | 56789012345 | 5365556677 | 2003-05-18 |

## 2) Başvurular (applications — her öğrenciye 1 tane)

| Öğrenci | currentUniversity | current/target Department | gpa | sayYksScore | sayYksRank | status | ydyoApproved | ydyoExamScore | ydyoNotes | submissionDate |
|---|---|---|---|---|---|---|---|---|---|---|
| Ayşe Demir | Ege Üniversitesi | Bilgisayar Mühendisliği | 3.45 | 478.250 | 11500 | `YDYO_REVIEW` | `null` | `null` | — | 2026-05-10 |
| Burak Çelik | Yıldız Teknik Üniversitesi | Bilgisayar Mühendisliği | 3.05 | 462.300 | 16800 | `YDYO_REVIEW` | `null` | `null` | — | 2026-05-12 |
| Mehmet Kaya | Dokuz Eylül Üniversitesi | Elektrik-Elektronik Mühendisliği | 3.10 | 465.500 | 16200 | `YDYO_EXAM_PENDING` | `false` | `null` | İngilizce yeterlilik belgesi eksik, sınava yönlendirildi. | 2026-05-08 |
| Selin Arslan | Marmara Üniversitesi | Elektrik-Elektronik Mühendisliği | 3.15 | 468.900 | 15300 | `YDYO_EXAM_PENDING` | `false` | `null` | İngilizce yeterlilik belgesi eksik, sınava yönlendirildi. | 2026-05-09 |
| Zeynep Şahin | Boğaziçi Üniversitesi | Makine Mühendisliği | 3.70 | 492.100 | 7800 | `YDYO_ACCEPTED` | `true` | `null` | Cambridge C1 ile muaf. | 2026-05-02 |
| Can Yıldız | Orta Doğu Teknik Üniversitesi | İnşaat Mühendisliği | 2.95 | 455.000 | 18900 | `YDYO_REJECTED` | `false` | `42` | Yeterlilik sınavı notu eşiğin altında. | 2026-04-28 |
| Elif Aydın | İstanbul Teknik Üniversitesi | Endüstri Mühendisliği | 3.30 | 470.750 | 14100 | `YDYO_ACCEPTED` | `false` | `72` | Belgesi yetersiz, sınavdan geçti — muaf. | 2026-05-05 |

## 3) Belgeler (documents)

Her başvuruda en az 1 **TRANSCRIPT** (`Transkript.pdf`). İngilizce belgesi olanlar → `documentType` **mutlaka `LANGUAGE_CERT`** olmalı (panel "İngilizce yeterlilik belgesi"ni bu tipten tanıyor):

| Öğrenci | Ek belge (`documentType = LANGUAGE_CERT`) |
|---|---|
| Ayşe Demir | `TOEFL_Belgesi.pdf` |
| Zeynep Şahin | `Cambridge_C1.pdf` |
| Diğer 5 | (yalnızca transkript) |

> Document zorunlu kolonları: `documentType`, `fileName`, `filePath`, `ydyoApproved` (bool), `oidbApproved` (default false), `documentUploadDate`, `active = true`.

## 4) status → panelde görünüm

| status | Görünüm |
|---|---|
| `YDYO_REVIEW` | Değerlendirilmemiş (taze) — Muafiyet: Beklemede |
| `YDYO_EXAM_PENDING` | Sınav Bekliyor — Muafiyet: Beklemede |
| `YDYO_ACCEPTED` | Muaf (ydyoApproved=true → belgeyle; false + puan≥60 → sınavla) |
| `YDYO_REJECTED` | Muaf Değil (puan<60) |

> Sınav geçme eşiği = **60** (`PASS_THRESHOLD`).

## 5) Test case → öğrenci eşlemesi

| Case | Öğrenci |
|---|---|
| TC-7.0 Belge onayı → Muaf | Ayşe Demir |
| TC-7.1 Açıklamasız kaydet (onay yolu) | Ayşe Demir |
| TC-7.2 Açıklamasız kaydet (red yolu) | Burak Çelik |
| TC-7.3 Reddet → Sınav Bekliyor | Burak Çelik |
| TC-7.4 Sınav puanı 72 → Muaf | Mehmet Kaya |
| TC-7.5 Sınav puanı 45 → Muaf Değil | Selin Arslan |
| TC-7.6 CSV dışa aktar (Tablo Oluştur) | — (dashboard) |
| TC-7.7 ÖİDB'ye İlet (pozitif) | tüm bekleyenler sonuçlanınca |
| TC-7.8 ÖİDB'ye İlet (Beklemede var) | Mehmet Kaya |
| TC-7.9 Muafiyet read-only | Ayşe Demir |
| TC-7.10 Puandan otomatik sonuç | Burak Çelik |
| TC-7.11 Belge onayı seçmeden kaydet | Ayşe Demir |
