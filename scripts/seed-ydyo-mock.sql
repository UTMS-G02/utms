-- ============================================================================
-- YDYO mock öğrencilerini gerçek Postgres'e seed'ler (src/api/ydyo.js ile birebir).
-- Real modda (VITE_USE_MOCK=false) liste mock'la aynı görünsün diye.
--
-- İDEMPOTENT: önce bu mock e-postalara ait her şeyi ve eski ad-hoc app #1/#2'yi
-- siler, sonra temiz seti ekler. Tekrar çalıştırınca çoğaltmaz.
-- ydyo@iyte.edu.tr (ve diğer mevcut kullanıcılar) KORUNUR.
--
-- Çalıştır:  PGPASSWORD=utms_pass psql -h localhost -U utms_user -d utms_db \
--               -f scripts/seed-ydyo-mock.sql
--
-- Badge map'i (backend toResponse türetimleri → frontend derive):
--   Beklemede            → YDYO_REVIEW,        ydyo_approved NULL
--   Onaylandı→Muaf       → YDYO_ACCEPTED,      ydyo_approved TRUE
--   Onaylanmadı→sınav    → YDYO_EXAM_PENDING,  ydyo_approved FALSE, puan yok
--   Onaylanmadı→Başarılı → YDYO_ACCEPTED,      ydyo_approved FALSE, puan>=60
--   Onaylanmadı→Başarısız→ YDYO_REJECTED,      ydyo_approved FALSE, puan<60
-- ============================================================================

BEGIN;

-- Mock öğrenci e-postaları (idempotency anahtarı)
\set emails '''ayse.demir@std.iyte.edu.tr'',''mehmet.kaya@std.iyte.edu.tr'',''zeynep.sahin@std.iyte.edu.tr'',''can.yildiz@std.iyte.edu.tr'',''elif.aydin@std.iyte.edu.tr'''

-- 0) Eski ad-hoc test başvurularını kaldır (app #1, #2) + belgeleri
DELETE FROM documents WHERE application_id IN (1, 2);
DELETE FROM applications WHERE application_id IN (1, 2);

-- 0b) TC çakışması: dummy 'teststudent@iyte.edu.tr' hesabını kaldır.
--     (TC 12345678901, mock Ayşe Demir ile birebir çakışıyor — kullanıcı kararıyla siliniyor.)
DELETE FROM documents WHERE application_id IN (
  SELECT application_id FROM applications WHERE user_id IN (SELECT user_id FROM users WHERE email = 'teststudent@iyte.edu.tr')
);
DELETE FROM applications WHERE user_id IN (SELECT user_id FROM users WHERE email = 'teststudent@iyte.edu.tr');
DELETE FROM students    WHERE user_id IN (SELECT user_id FROM users WHERE email = 'teststudent@iyte.edu.tr');
DELETE FROM users       WHERE email = 'teststudent@iyte.edu.tr';

-- 1) İdempotency: mock e-postalara ait belge → başvuru → student → user sil
DELETE FROM documents WHERE application_id IN (
  SELECT a.application_id FROM applications a
  JOIN users u ON u.user_id = a.user_id
  WHERE u.email IN (:emails)
);
DELETE FROM applications WHERE user_id IN (SELECT user_id FROM users WHERE email IN (:emails));
DELETE FROM students    WHERE user_id IN (SELECT user_id FROM users WHERE email IN (:emails));
DELETE FROM users       WHERE email IN (:emails);

-- 2) 5 öğrenciyi ekle. Tüm seed öğrenciler ydyo@ ile aynı bcrypt'i paylaşır (test123).

-- (a) Ayşe Demir — Beklemede (henüz inceleme) → YDYO_REVIEW
WITH u AS (
  INSERT INTO users (email, password_hash, first_name, last_name, role, active)
  VALUES ('ayse.demir@std.iyte.edu.tr',
          (SELECT password_hash FROM users WHERE email = 'ydyo@iyte.edu.tr'),
          'Ayşe', 'Demir', 'STUDENT', true)
  RETURNING user_id
), s AS (
  INSERT INTO students (user_id, tckn, phone_number)
  SELECT user_id, '12345678901', '0532 111 22 33' FROM u RETURNING user_id
), a AS (
  INSERT INTO applications (user_id, academic_year, semester, status, target_department, target_faculty,
    say_yks_score, say_yks_rank, current_university, current_faculty, current_department, gpa,
    created_at, submission_date, revision_requested_before, ydyo_approved, ydyo_exam_score, ydyo_notes, ydyo_reviewed_date)
  SELECT user_id, '2025-2026', 'FALL', 'YDYO_REVIEW', 'Bilgisayar Mühendisliği', 'Mühendislik Fakültesi',
    480.5, 1500, 'Ege Üniversitesi', 'Mühendislik Fakültesi', 'Bilgisayar Mühendisliği', 3.20,
    TIMESTAMP '2026-05-10 09:20:00', DATE '2026-05-10', false, NULL, NULL, NULL, NULL
  FROM s RETURNING application_id
)
INSERT INTO documents (application_id, document_type, file_name, file_path, active, oidb_approved, ydyo_approved, document_upload_date)
SELECT application_id, d.dtype, d.fname, d.fpath, true, false, false, DATE '2026-05-10'
FROM a, (VALUES
  ('TRANSCRIPT',    'Transkript.pdf',    '/files/ayse_transkript.pdf'),
  ('APPROVAL',      'Onay_Belgesi.pdf',  '/files/ayse_onay.pdf'),
  ('LANGUAGE_CERT', 'TOEFL_Belgesi.pdf', '/files/ayse_toefl.pdf')
) AS d(dtype, fname, fpath);

-- (b) Mehmet Kaya — Onaylanmadı → sınav bekliyor → YDYO_EXAM_PENDING
WITH u AS (
  INSERT INTO users (email, password_hash, first_name, last_name, role, active)
  VALUES ('mehmet.kaya@std.iyte.edu.tr',
          (SELECT password_hash FROM users WHERE email = 'ydyo@iyte.edu.tr'),
          'Mehmet', 'Kaya', 'STUDENT', true)
  RETURNING user_id
), s AS (
  INSERT INTO students (user_id, tckn, phone_number)
  SELECT user_id, '23456789012', '0533 222 33 44' FROM u RETURNING user_id
), a AS (
  INSERT INTO applications (user_id, academic_year, semester, status, target_department, target_faculty,
    say_yks_score, say_yks_rank, current_university, current_faculty, current_department, gpa,
    created_at, submission_date, revision_requested_before, ydyo_approved, ydyo_exam_score, ydyo_notes, ydyo_reviewed_date)
  SELECT user_id, '2025-2026', 'FALL', 'YDYO_EXAM_PENDING', 'Elektrik-Elektronik Mühendisliği', 'Mühendislik Fakültesi',
    465.0, 2200, 'Dokuz Eylül Üniversitesi', 'Mühendislik Fakültesi', 'Elektrik-Elektronik Mühendisliği', 2.95,
    TIMESTAMP '2026-05-08 14:05:00', DATE '2026-05-08', false, false, NULL,
    'İngilizce yeterlilik belgesi eksik, sınava yönlendirildi.', TIMESTAMP '2026-05-09 10:00:00'
  FROM s RETURNING application_id
)
INSERT INTO documents (application_id, document_type, file_name, file_path, active, oidb_approved, ydyo_approved, document_upload_date)
SELECT application_id, 'TRANSCRIPT', 'Transkript.pdf', '/files/mehmet_transkript.pdf', true, false, false, DATE '2026-05-08'
FROM a;

-- (c) Zeynep Şahin — Onaylandı → Muaf → YDYO_ACCEPTED (ydyo_approved TRUE)
WITH u AS (
  INSERT INTO users (email, password_hash, first_name, last_name, role, active)
  VALUES ('zeynep.sahin@std.iyte.edu.tr',
          (SELECT password_hash FROM users WHERE email = 'ydyo@iyte.edu.tr'),
          'Zeynep', 'Şahin', 'STUDENT', true)
  RETURNING user_id
), s AS (
  INSERT INTO students (user_id, tckn, phone_number)
  SELECT user_id, '34567890123', '0534 333 44 55' FROM u RETURNING user_id
), a AS (
  INSERT INTO applications (user_id, academic_year, semester, status, target_department, target_faculty,
    say_yks_score, say_yks_rank, current_university, current_faculty, current_department, gpa,
    created_at, submission_date, revision_requested_before, ydyo_approved, ydyo_exam_score, ydyo_notes, ydyo_reviewed_date)
  SELECT user_id, '2025-2026', 'FALL', 'YDYO_ACCEPTED', 'Makine Mühendisliği', 'Mühendislik Fakültesi',
    495.0, 800, 'Boğaziçi Üniversitesi', 'Mühendislik Fakültesi', 'Makine Mühendisliği', 3.65,
    TIMESTAMP '2026-05-02 11:40:00', DATE '2026-05-02', false, true, NULL,
    'Cambridge C1 ile muaf.', TIMESTAMP '2026-05-03 09:30:00'
  FROM s RETURNING application_id
)
INSERT INTO documents (application_id, document_type, file_name, file_path, active, oidb_approved, ydyo_approved, document_upload_date)
SELECT application_id, d.dtype, d.fname, d.fpath, true, false, false, DATE '2026-05-02'
FROM a, (VALUES
  ('TRANSCRIPT',    'Transkript.pdf',   '/files/zeynep_transkript.pdf'),
  ('LANGUAGE_CERT', 'Cambridge_C1.pdf', '/files/zeynep_cambridge.pdf')
) AS d(dtype, fname, fpath);

-- (d) Can Yıldız — Onaylanmadı → Başarısız → Muaf Değil → YDYO_REJECTED (puan 42)
WITH u AS (
  INSERT INTO users (email, password_hash, first_name, last_name, role, active)
  VALUES ('can.yildiz@std.iyte.edu.tr',
          (SELECT password_hash FROM users WHERE email = 'ydyo@iyte.edu.tr'),
          'Can', 'Yıldız', 'STUDENT', true)
  RETURNING user_id
), s AS (
  INSERT INTO students (user_id, tckn, phone_number)
  SELECT user_id, '45678901234', '0535 444 55 66' FROM u RETURNING user_id
), a AS (
  INSERT INTO applications (user_id, academic_year, semester, status, target_department, target_faculty,
    say_yks_score, say_yks_rank, current_university, current_faculty, current_department, gpa,
    created_at, submission_date, revision_requested_before, ydyo_approved, ydyo_exam_score, ydyo_notes, ydyo_reviewed_date)
  SELECT user_id, '2025-2026', 'FALL', 'YDYO_REJECTED', 'İnşaat Mühendisliği', 'Mühendislik Fakültesi',
    455.0, 3100, 'Orta Doğu Teknik Üniversitesi', 'Mühendislik Fakültesi', 'İnşaat Mühendisliği', 2.70,
    TIMESTAMP '2026-04-28 08:15:00', DATE '2026-04-28', false, false, 42,
    'Yeterlilik sınavı notu eşiğin altında.', TIMESTAMP '2026-04-29 11:00:00'
  FROM s RETURNING application_id
)
INSERT INTO documents (application_id, document_type, file_name, file_path, active, oidb_approved, ydyo_approved, document_upload_date)
SELECT application_id, 'TRANSCRIPT', 'Transkript.pdf', '/files/can_transkript.pdf', true, false, false, DATE '2026-04-28'
FROM a;

-- (e) Elif Aydın — Onaylanmadı → Başarılı → Muaf → YDYO_ACCEPTED (ydyo_approved FALSE, puan 72)
WITH u AS (
  INSERT INTO users (email, password_hash, first_name, last_name, role, active)
  VALUES ('elif.aydin@std.iyte.edu.tr',
          (SELECT password_hash FROM users WHERE email = 'ydyo@iyte.edu.tr'),
          'Elif', 'Aydın', 'STUDENT', true)
  RETURNING user_id
), s AS (
  INSERT INTO students (user_id, tckn, phone_number)
  SELECT user_id, '56789012345', '0536 555 66 77' FROM u RETURNING user_id
), a AS (
  INSERT INTO applications (user_id, academic_year, semester, status, target_department, target_faculty,
    say_yks_score, say_yks_rank, current_university, current_faculty, current_department, gpa,
    created_at, submission_date, revision_requested_before, ydyo_approved, ydyo_exam_score, ydyo_notes, ydyo_reviewed_date)
  SELECT user_id, '2025-2026', 'FALL', 'YDYO_ACCEPTED', 'Endüstri Mühendisliği', 'Mühendislik Fakültesi',
    472.0, 1900, 'İstanbul Teknik Üniversitesi', 'Mühendislik Fakültesi', 'Endüstri Mühendisliği', 3.10,
    TIMESTAMP '2026-05-05 10:30:00', DATE '2026-05-05', false, false, 72,
    'Belgesi yetersiz, sınavdan geçti — muaf.', TIMESTAMP '2026-05-06 13:15:00'
  FROM s RETURNING application_id
)
INSERT INTO documents (application_id, document_type, file_name, file_path, active, oidb_approved, ydyo_approved, document_upload_date)
SELECT application_id, 'TRANSCRIPT', 'Transkript.pdf', '/files/elif_transkript.pdf', true, false, false, DATE '2026-05-05'
FROM a;

COMMIT;
