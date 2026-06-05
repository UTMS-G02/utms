## **UC-7 TESTS** {#uc-7-tests}

> Reflects the latest YDYO panel implementation. Exam results are entered **per student** via the numeric 'Sınav Puanı' field (result and exemption are auto-derived, read-only; pass threshold = 60). The dashboard exports the list ('Tablo Oluştur') and transmits it to ÖİDB; there is **no bulk CSV upload**. UI buttons/labels/messages are in Turkish; descriptions are in English.
>
> **Fixed test students** (seed data — see `ydyo-test-data.md` for full field values): **Ayşe Demir** (not-yet-evaluated, valid TOEFL) · **Burak Çelik** (not-yet-evaluated, no certificate) · **Mehmet Kaya** (Sınav Bekliyor, no certificate) · **Selin Arslan** (Sınav Bekliyor, no certificate) · **Zeynep Şahin** (already Muaf via Cambridge) · **Can Yıldız** (already rejected, exam 42) · **Elif Aydın** (already Muaf via exam 72).

| Test Scenario ID |  | UC7-Manage English Proficiency Assessment |  |  | Test Case ID |  |  | TC-7.0 |  |  |  |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | ----- |
| **Test Case Description** |  | Document Approval – Positive (Exempt Path) |  |  | **Test Priority** |  |  | High |  |  |  |
| **Pre-Requisite** |  | PRE-1: Transfer applications forwarded to YDYO by ÖİDB. PRE-2: YDYO staff is logged into UTMS. |  |  | **Post-Requisite** |  |  | POST-1: Student Muafiyet Sonucu \= 'Muaf'. POST-2: Açıklama is filled. Record saved. |  |  |  |
| **Designed-by** |  | Sıla Kırılmaz |  |  | **Executed-by** |  |  |  |  |  |  |
| **Design date** |  | 09.04.2026 |  |  | **Execution date** |  |  |  |  |  |  |
| Test Execution Steps: |  |  |  |  |  |  |  |  |  |  |  |
| **S.No** | **Action** |  | **Inputs** | **Expected Output** |  | **Actual Output** | **Test Environment** |  | **Test Result** | **Test Comments** |  |
| 1 | Navigate to YDYO Dashboard |  | YDYO staff logs in and opens the student list | Dashboard loads; student list with pending applications is displayed. |  |  |  |  |  |  |  |
| 2 | Select a student record |  | Click on 'Ayşe Demir' (valid TOEFL certificate, not yet evaluated) | Student detail page opens; documents and 'Belge Değerlendirme' fields are visible. |  |  |  |  |  |  |  |
| 3 | Review proficiency document |  | Click the document link to open the proficiency certificate in the browser viewer | PDF renders natively in browser within ≤10 seconds; no external plugin required. |  |  |  |  |  |  |  |
| 4 | Set 'Belge Onay Durumu' to 'Onaylandı' |  | Change the 'Belge Onay Durumu' dropdown to 'Onaylandı' | 'Sınav Sonucu' auto-set to 'Sınav Gerekli Değil'; 'Muafiyet Sonucu' auto-set to 'Muaf'. Both fields become read-only. |  |  |  |  |  |  |  |
| 5 | Enter explanation note |  | Type 'Cambridge C1 ile muaf' in the 'Açıklama' field | Text accepted; 'Kaydet' button remains enabled. |  |  |  |  |  |  |  |
| 6 | Save the record |  | Click 'Kaydet' | System shows 'Değerlendirme kaydedildi.' and returns to the student list. Student's Muafiyet Sonucu \= 'Muaf'. |  |  |  |  |  |  |  |

| Test Scenario ID |  | UC7-Manage English Proficiency Assessment |  |  | Test Case ID |  |  | TC-7.1 |  |  |  |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | ----- |
| **Test Case Description** |  | Save Without Açıklama – Negative (Exempt Path) |  |  | **Test Priority** |  |  | High |  |  |  |
| **Pre-Requisite** |  | PRE-1: 'Ayşe Demir' record is open. PRE-2: 'Belge Onay Durumu' \= 'Onaylandı'. Açıklama is empty. |  |  | **Post-Requisite** |  |  | POST: Record NOT saved. Warning message shown. |  |  |  |
| **Designed-by** |  | Sıla Kırılmaz |  |  | **Executed-by** |  |  |  |  |  |  |
| **Design date** |  | 09.04.2026 |  |  | **Execution date** |  |  |  |  |  |  |
| Test Execution Steps: |  |  |  |  |  |  |  |  |  |  |  |
| **S.No** | **Action** |  | **Inputs** | **Expected Output** |  | **Actual Output** | **Test Environment** |  | **Test Result** | **Test Comments** |  |
| 1 | Open a student record |  | Open 'Ayşe Demir' from the list | Detail page opens with the evaluation fields editable. |  |  |  |  |  |  |  |
| 2 | Set 'Belge Onay Durumu' to 'Onaylandı' |  | Change dropdown to 'Onaylandı' | 'Sınav Sonucu' \= 'Sınav Gerekli Değil'; 'Muafiyet Sonucu' \= 'Muaf'. |  |  |  |  |  |  |  |
| 3 | Leave Açıklama empty |  | Do NOT enter any text in 'Açıklama' | 'Açıklama' field remains blank. |  |  |  |  |  |  |  |
| 4 | Attempt to save |  | Click 'Kaydet' | System blocks the save and displays warning: 'Lütfen açıklama giriniz.' Record NOT saved. User stays on detail page. |  |  |  |  |  |  |  |

| Test Scenario ID |  | UC7-Manage English Proficiency Assessment |  |  | Test Case ID |  |  | TC-7.2 |  |  |  |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | ----- |
| **Test Case Description** |  | Save Without Açıklama – Negative (Rejection Path) |  |  | **Test Priority** |  |  | High |  |  |  |
| **Pre-Requisite** |  | PRE-1: 'Burak Çelik' record is open. PRE-2: 'Belge Onay Durumu' \= 'Onaylanmadı', 'Sınav Puanı' empty. Açıklama is empty. |  |  | **Post-Requisite** |  |  | POST: Record NOT saved. Warning message shown. |  |  |  |
| **Designed-by** |  | Sıla Kırılmaz |  |  | **Executed-by** |  |  |  |  |  |  |
| **Design date** |  | 09.04.2026 |  |  | **Execution date** |  |  |  |  |  |  |
| Test Execution Steps: |  |  |  |  |  |  |  |  |  |  |  |
| **S.No** | **Action** |  | **Inputs** | **Expected Output** |  | **Actual Output** | **Test Environment** |  | **Test Result** | **Test Comments** |  |
| 1 | Open a student record |  | Open 'Burak Çelik' from the list (no proficiency certificate) | Detail page opens; fields editable. |  |  |  |  |  |  |  |
| 2 | Set 'Belge Onay Durumu' to 'Onaylanmadı' |  | Change dropdown to 'Onaylanmadı'; leave 'Sınav Puanı' empty | 'Sınav Puanı' input becomes active; 'Sınav Sonucu' \= 'Sınav Sonucu Bekleniyor'; 'Muafiyet Sonucu' \= 'Beklemede'. |  |  |  |  |  |  |  |
| 3 | Leave Açıklama empty |  | Do NOT enter any text in 'Açıklama' | 'Açıklama' field remains blank. |  |  |  |  |  |  |  |
| 4 | Attempt to save |  | Click 'Kaydet' | System blocks the save and displays warning: 'Lütfen açıklama giriniz.' Record NOT saved. User stays on detail page. |  |  |  |  |  |  |  |

| Test Scenario ID |  | UC7-Manage English Proficiency Assessment |  |  | Test Case ID |  |  | TC-7.3 |  |  |  |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | ----- |
| **Test Case Description** |  | Document Rejection → Exam Pending (No Score) – Positive |  |  | **Test Priority** |  |  | High |  |  |  |
| **Pre-Requisite** |  | PRE-1: Applications forwarded to YDYO. PRE-2: YDYO staff logged in. PRE-3: 'Burak Çelik' has no/invalid proficiency document. |  |  | **Post-Requisite** |  |  | POST-1: Student Muafiyet Sonucu \= 'Beklemede'. POST-2: Açıklama filled. Record saved. |  |  |  |
| **Designed-by** |  | Sıla Kırılmaz |  |  | **Executed-by** |  |  |  |  |  |  |
| **Design date** |  | 09.04.2026 |  |  | **Execution date** |  |  |  |  |  |  |
| Test Execution Steps: |  |  |  |  |  |  |  |  |  |  |  |
| **S.No** | **Action** |  | **Inputs** | **Expected Output** |  | **Actual Output** | **Test Environment** |  | **Test Result** | **Test Comments** |  |
| 1 | Open a student record |  | Select 'Burak Çelik' (no/invalid proficiency document) | Student detail page opens with evaluation fields editable. |  |  |  |  |  |  |  |
| 2 | Set 'Belge Onay Durumu' to 'Onaylanmadı' |  | Change dropdown to 'Onaylanmadı'; leave 'Sınav Puanı' empty | 'Sınav Sonucu' auto-set to 'Sınav Sonucu Bekleniyor'; 'Muafiyet Sonucu' auto-set to 'Beklemede'. |  |  |  |  |  |  |  |
| 3 | Enter rejection reason in Açıklama |  | Type a rejection reason, e.g. 'Geçerli dil belgesi sunulmamıştır' | Text accepted; 'Kaydet' enabled. |  |  |  |  |  |  |  |
| 4 | Save the record |  | Click 'Kaydet' | System shows 'Değerlendirme kaydedildi.' Record saved with Muafiyet Sonucu \= 'Beklemede'. System returns to the student list. |  |  |  |  |  |  |  |

| Test Scenario ID |  | UC7-Manage English Proficiency Assessment |  |  | Test Case ID |  |  | TC-7.4 |  |  |  |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | ----- |
| **Test Case Description** |  | Exam Score Entry – Pass (Score ≥ 60) – Positive |  |  | **Test Priority** |  |  | High |  |  |  |
| **Pre-Requisite** |  | PRE-1: 'Mehmet Kaya' is in 'Sınav Bekliyor' (directed to the proficiency exam). PRE-2: YDYO staff logged in. |  |  | **Post-Requisite** |  |  | POST-1: Sınav Sonucu \= 'Sınav Başarılı'. POST-2: Muafiyet Sonucu \= 'Muaf'. Record saved. |  |  |  |
| **Designed-by** |  | Sıla Kırılmaz |  |  | **Executed-by** |  |  |  |  |  |  |
| **Design date** |  | 09.04.2026 |  |  | **Execution date** |  |  |  |  |  |  |
| Test Execution Steps: |  |  |  |  |  |  |  |  |  |  |  |
| **S.No** | **Action** |  | **Inputs** | **Expected Output** |  | **Actual Output** | **Test Environment** |  | **Test Result** | **Test Comments** |  |
| 1 | Open the student record |  | Select 'Mehmet Kaya' (directed to the proficiency exam) | Student detail page opens. |  |  |  |  |  |  |  |
| 2 | Set 'Belge Onay Durumu' to 'Onaylanmadı' |  | Change dropdown to 'Onaylanmadı' | 'Sınav Puanı' input becomes active (range 0–100; pass threshold 60). |  |  |  |  |  |  |  |
| 3 | Enter a passing score |  | Type '72' in 'Sınav Puanı' | 'Sınav Sonucu' auto-set to 'Sınav Başarılı'; 'Muafiyet Sonucu' auto-set to 'Muaf'. |  |  |  |  |  |  |  |
| 4 | Enter explanation and save |  | Type a note in 'Açıklama' and click 'Kaydet' | System shows 'Değerlendirme kaydedildi.' Muafiyet Sonucu \= 'Muaf'. |  |  |  |  |  |  |  |

| Test Scenario ID |  | UC7-Manage English Proficiency Assessment |  |  | Test Case ID |  |  | TC-7.5 |  |  |  |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | ----- |
| **Test Case Description** |  | Exam Score Entry – Fail (Score < 60) – Positive |  |  | **Test Priority** |  |  | High |  |  |  |
| **Pre-Requisite** |  | PRE-1: 'Selin Arslan' is in 'Sınav Bekliyor'. PRE-2: YDYO staff logged in. |  |  | **Post-Requisite** |  |  | POST-1: Sınav Sonucu \= 'Sınav Başarısız'. POST-2: Muafiyet Sonucu \= 'Muaf Değil'. Record saved. |  |  |  |
| **Designed-by** |  | Sıla Kırılmaz |  |  | **Executed-by** |  |  |  |  |  |  |
| **Design date** |  | 09.04.2026 |  |  | **Execution date** |  |  |  |  |  |  |
| Test Execution Steps: |  |  |  |  |  |  |  |  |  |  |  |
| **S.No** | **Action** |  | **Inputs** | **Expected Output** |  | **Actual Output** | **Test Environment** |  | **Test Result** | **Test Comments** |  |
| 1 | Open the student record |  | Select 'Selin Arslan' (directed to the proficiency exam) | Student detail page opens. |  |  |  |  |  |  |  |
| 2 | Set 'Belge Onay Durumu' to 'Onaylanmadı' |  | Change dropdown to 'Onaylanmadı' | 'Sınav Puanı' input becomes active. |  |  |  |  |  |  |  |
| 3 | Enter a failing score |  | Type '45' in 'Sınav Puanı' | 'Sınav Sonucu' auto-set to 'Sınav Başarısız'; 'Muafiyet Sonucu' auto-set to 'Muaf Değil'. |  |  |  |  |  |  |  |
| 4 | Enter explanation and save |  | Type a note in 'Açıklama' and click 'Kaydet' | System shows 'Değerlendirme kaydedildi.' Muafiyet Sonucu \= 'Muaf Değil'. |  |  |  |  |  |  |  |

| Test Scenario ID |  | UC7-Manage English Proficiency Assessment |  |  | Test Case ID |  |  | TC-7.6 |  |  |  |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | ----- |
| **Test Case Description** |  | Export Application List to CSV ('Tablo Oluştur') – Positive |  |  | **Test Priority** |  |  | Medium |  |  |  |
| **Pre-Requisite** |  | PRE-1: YDYO staff logged in. PRE-2: The dashboard shows at least one application. |  |  | **Post-Requisite** |  |  | POST: A .csv file of the visible list is downloaded. Confirmation message shown. |  |  |  |
| **Designed-by** |  | Sıla Kırılmaz |  |  | **Executed-by** |  |  |  |  |  |  |
| **Design date** |  | 09.04.2026 |  |  | **Execution date** |  |  |  |  |  |  |
| Test Execution Steps: |  |  |  |  |  |  |  |  |  |  |  |
| **S.No** | **Action** |  | **Inputs** | **Expected Output** |  | **Actual Output** | **Test Environment** |  | **Test Result** | **Test Comments** |  |
| 1 | Open YDYO Dashboard |  | YDYO staff opens the dashboard | Application list is displayed; 'Tablo Oluştur' button is visible. |  |  |  |  |  |  |  |
| 2 | Click 'Tablo Oluştur' |  | Click the 'Tablo Oluştur' button | The currently visible list is exported and a '.csv' file is downloaded to the device. |  |  |  |  |  |  |  |
| 3 | Verify confirmation |  | Observe the UI after the download | UI displays: 'Liste CSV olarak indirildi.' |  |  |  |  |  |  |  |

| Test Scenario ID |  | UC7-Manage English Proficiency Assessment |  |  | Test Case ID |  |  | TC-7.7 |  |  |  |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | ----- |
| **Test Case Description** |  | Transmit Final List to ÖİDB – Positive (Phase 3) |  |  | **Test Priority** |  |  | High |  |  |  |
| **Pre-Requisite** |  | PRE-1: All students are finalized — no 'Beklemede' (i.e. Ayşe Demir, Burak Çelik, Mehmet Kaya and Selin Arslan have all been evaluated). PRE-2: YDYO staff logged in. |  |  | **Post-Requisite** |  |  | POST-1: List transmitted; records become read-only. POST-2: Confirmation message displayed. |  |  |  |
| **Designed-by** |  | Sıla Kırılmaz |  |  | **Executed-by** |  |  |  |  |  |  |
| **Design date** |  | 09.04.2026 |  |  | **Execution date** |  |  |  |  |  |  |
| Test Execution Steps: |  |  |  |  |  |  |  |  |  |  |  |
| **S.No** | **Action** |  | **Inputs** | **Expected Output** |  | **Actual Output** | **Test Environment** |  | **Test Result** | **Test Comments** |  |
| 1 | Verify all records finalized |  | Review dashboard; confirm zero 'Beklemede' entries exist | 'Sonuçları ÖİDB'ye İlet' button is enabled. |  |  |  |  |  |  |  |
| 2 | Click 'Sonuçları ÖİDB'ye İlet' |  | Click the transmission button on the dashboard | A confirmation dialog appears; confirm with 'İlet'. |  |  |  |  |  |  |  |
| 3 | Confirm transmission |  | Observe system processing | System validates that no 'Beklemede' records exist and completes the transmission. |  |  |  |  |  |  |  |
| 4 | Verify success message |  | Observe UI after transmission | UI displays: 'Liste başarıyla ÖİDB'ye iletildi.' The button locks to 'ÖİDB'ye İletildi' and the list is no longer editable. |  |  |  |  |  |  |  |

| Test Scenario ID |  | UC7-Manage English Proficiency Assessment |  |  | Test Case ID |  |  | TC-7.8 |  |  |  |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | ----- |
| **Test Case Description** |  | Incomplete List Transmission – Negative |  |  | **Test Priority** |  |  | High |  |  |  |
| **Pre-Requisite** |  | PRE-1: 'Mehmet Kaya' has Muafiyet Sonucu \= 'Beklemede' (Sınav Bekliyor). PRE-2: YDYO staff logged in. |  |  | **Post-Requisite** |  |  | POST: Transmission NOT executed. Blocking error message shown. |  |  |  |
| **Designed-by** |  | Sıla Kırılmaz |  |  | **Executed-by** |  |  |  |  |  |  |
| **Design date** |  | 09.04.2026 |  |  | **Execution date** |  |  |  |  |  |  |
| Test Execution Steps: |  |  |  |  |  |  |  |  |  |  |  |
| **S.No** | **Action** |  | **Inputs** | **Expected Output** |  | **Actual Output** | **Test Environment** |  | **Test Result** | **Test Comments** |  |
| 1 | Confirm a pending student exists |  | Dashboard shows 'Mehmet Kaya' with 'Beklemede' status | The list contains an incomplete record. |  |  |  |  |  |  |  |
| 2 | Attempt transmission |  | Click 'Sonuçları ÖİDB'ye İlet' | System detects incomplete records and blocks the action. UI displays: 'Tüm öğrenci kayıtları tamamlanmadan liste iletilemez.' No data is sent to ÖİDB. |  |  |  |  |  |  |  |

| Test Scenario ID |  | UC7-Manage English Proficiency Assessment |  |  | Test Case ID |  |  | TC-7.9 |  |  |  |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | ----- |
| **Test Case Description** |  | Muafiyet Sonucu Read-Only Verification |  |  | **Test Priority** |  |  | Medium |  |  |  |
| **Pre-Requisite** |  | PRE-1: 'Ayşe Demir' record is open. PRE-2: YDYO staff logged in. |  |  | **Post-Requisite** |  |  | POST: 'Muafiyet Sonucu' field does not accept any manual input. |  |  |  |
| **Designed-by** |  | Sıla Kırılmaz |  |  | **Executed-by** |  |  |  |  |  |  |
| **Design date** |  | 09.04.2026 |  |  | **Execution date** |  |  |  |  |  |  |
| Test Execution Steps: |  |  |  |  |  |  |  |  |  |  |  |
| **S.No** | **Action** |  | **Inputs** | **Expected Output** |  | **Actual Output** | **Test Environment** |  | **Test Result** | **Test Comments** |  |
| 1 | Open student record |  | Select 'Ayşe Demir' from the YDYO dashboard | Detail page opens; 'Muafiyet Sonucu' field visible. |  |  |  |  |  |  |  |
| 2 | Attempt to directly edit Muafiyet Sonucu |  | Try to click or type in the 'Muafiyet Sonucu' field | Field is non-interactive (read-only). No direct input is accepted. |  |  |  |  |  |  |  |
| 3 | Change Belge Onay Durumu and observe auto-update |  | Switch 'Belge Onay Durumu' between 'Onaylandı' and 'Onaylanmadı' | 'Muafiyet Sonucu' updates automatically based on system logic only; the user cannot override it. |  |  |  |  |  |  |  |

| Test Scenario ID |  | UC7-Manage English Proficiency Assessment |  |  | Test Case ID |  |  | TC-7.10 |  |  |  |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | ----- |
| **Test Case Description** |  | Auto-Derived Result from 'Sınav Puanı' |  |  | **Test Priority** |  |  | Medium |  |  |  |
| **Pre-Requisite** |  | PRE-1: 'Burak Çelik' record is open. PRE-2: YDYO staff logged in. |  |  | **Post-Requisite** |  |  | POST: 'Sınav Sonucu' and 'Muafiyet Sonucu' derive correctly from the entered score (pass threshold \= 60). |  |  |  |
| **Designed-by** |  | Sıla Kırılmaz |  |  | **Executed-by** |  |  |  |  |  |  |
| **Design date** |  | 09.04.2026 |  |  | **Execution date** |  |  |  |  |  |  |
| Test Execution Steps: |  |  |  |  |  |  |  |  |  |  |  |
| **S.No** | **Action** |  | **Inputs** | **Expected Output** |  | **Actual Output** | **Test Environment** |  | **Test Result** | **Test Comments** |  |
| 1 | Set Belge Onay Durumu to 'Onaylandı' |  | Select 'Onaylandı' | 'Sınav Sonucu' auto-set to 'Sınav Gerekli Değil' (no score input shown); 'Muafiyet Sonucu' \= 'Muaf'. |  |  |  |  |  |  |  |
| 2 | Set Belge Onay Durumu to 'Onaylanmadı' |  | Change to 'Onaylanmadı'; leave score empty | 'Sınav Puanı' input becomes active; 'Sınav Sonucu' \= 'Sınav Sonucu Bekleniyor'; 'Muafiyet Sonucu' \= 'Beklemede'. |  |  |  |  |  |  |  |
| 3 | Enter a passing score |  | Type '72' in 'Sınav Puanı' | 'Sınav Sonucu' auto-updates to 'Sınav Başarılı'; 'Muafiyet Sonucu' \= 'Muaf'. |  |  |  |  |  |  |  |
| 4 | Enter a failing score |  | Change 'Sınav Puanı' to '45' | 'Sınav Sonucu' auto-updates to 'Sınav Başarısız'; 'Muafiyet Sonucu' \= 'Muaf Değil'. |  |  |  |  |  |  |  |

| Test Scenario ID |  | UC7-Manage English Proficiency Assessment |  |  | Test Case ID |  |  | TC-7.11 |  |  |  |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | ----- |
| **Test Case Description** |  | Save Without Belge Onay Durumu – Negative |  |  | **Test Priority** |  |  | Medium |  |  |  |
| **Pre-Requisite** |  | PRE-1: 'Ayşe Demir' record is open. PRE-2: 'Belge Onay Durumu' is left unselected ('Seçiniz'). |  |  | **Post-Requisite** |  |  | POST: Record NOT saved. Warning message shown. |  |  |  |
| **Designed-by** |  | Sıla Kırılmaz |  |  | **Executed-by** |  |  |  |  |  |  |
| **Design date** |  | 09.04.2026 |  |  | **Execution date** |  |  |  |  |  |  |
| Test Execution Steps: |  |  |  |  |  |  |  |  |  |  |  |
| **S.No** | **Action** |  | **Inputs** | **Expected Output** |  | **Actual Output** | **Test Environment** |  | **Test Result** | **Test Comments** |  |
| 1 | Open a student record |  | Open 'Ayşe Demir' from the list | Detail page opens; 'Belge Onay Durumu' shows the placeholder 'Seçiniz'. |  |  |  |  |  |  |  |
| 2 | Enter an explanation only |  | Type any text in 'Açıklama' without selecting 'Belge Onay Durumu' | Text is accepted in 'Açıklama'. |  |  |  |  |  |  |  |
| 3 | Attempt to save |  | Click 'Kaydet' | System blocks the save and displays warning: 'Lütfen belge onay durumunu seçiniz.' Record NOT saved. |  |  |  |  |  |  |  |

#### 
