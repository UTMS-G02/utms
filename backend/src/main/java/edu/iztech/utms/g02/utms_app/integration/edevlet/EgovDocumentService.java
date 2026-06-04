package edu.iztech.utms.g02.utms_app.integration.edevlet;

import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Document;
import edu.iztech.utms.g02.utms_app.dal.application.repository.DocumentRepository;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Student;
import edu.iztech.utms.g02.utms_app.integration.yoksis.dto.YoksisStudentResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock e-Devlet / ÖSYM belge sağlayıcısı.
 *
 * Gerçek bir öğrenci projesinde e-Devlet ve ÖSYM resmi API'lerine erişim mümkün
 * olmadığından, bu servis "otomatik çekilmiş" belgeleri (öğrenci belgesi, transkript,
 * YKS sonuç belgesi) yer-tutucu PDF'ler olarak ÜRETİR ve başvuruya iliştirir.
 * Böylece öğrenci bu belgeleri elle yüklemek zorunda kalmaz; arayüzde
 * "e-Devlet/ÖSYM'den otomatik alındı" olarak gösterilir. Tamamen simülasyondur.
 */
@Service
@RequiredArgsConstructor
public class EgovDocumentService {

    private static final Path UPLOAD_DIRECTORY = Paths.get("uploads");

    private final DocumentRepository documentRepository;

    /**
     * Başvuru için e-Devlet/ÖSYM'den "çekilmiş" belgeleri üretir ve kaydeder.
     * create() sonrası, başvuru DRAFT durumundayken çağrılır.
     */
    public void generateAndAttach(Application app, Student student, YoksisStudentResponse yoksis) {
        String fullName = asciify(buildFullName(student));
        String tckn = student.getTckn() == null ? "-" : student.getTckn();

        attach(app, "STUDENT_CERTIFICATE", "ogrenci_belgesi.pdf", List.of(
                "T.C. e-Devlet Kapisi - Ogrenci Belgesi (Mock)",
                "",
                "Ad Soyad : " + fullName,
                "TCKN     : " + tckn,
                "Universite: " + asciify(safe(yoksis.currentUniversity())),
                "Fakulte  : " + asciify(safe(yoksis.currentFaculty())),
                "Bolum    : " + asciify(safe(yoksis.currentDepartment())),
                "Yariyil  : " + safe(yoksis.semester()),
                "",
                "Bu belge e-Devlet uzerinden otomatik olusturulmustur (simulasyon)."
        ));

        attach(app, "TRANSCRIPT", "transkript.pdf", List.of(
                "T.C. e-Devlet Kapisi - Not Dokumu / Transkript (Mock)",
                "",
                "Ad Soyad : " + fullName,
                "TCKN     : " + tckn,
                "Bolum    : " + asciify(safe(yoksis.currentDepartment())),
                "Genel Not Ortalamasi (GNO): " + safe(yoksis.gpa()),
                "",
                "Bu belge e-Devlet uzerinden otomatik olusturulmustur (simulasyon)."
        ));

        attach(app, "YKS_RESULT", "yks_sonuc_belgesi.pdf", List.of(
                "OSYM - YKS Sonuc Belgesi (Mock)",
                "",
                "Ad Soyad : " + fullName,
                "TCKN     : " + tckn,
                "SAY Puani     : " + safe(yoksis.yksScore()),
                "SAY Siralamasi: " + safe(yoksis.yksRank()),
                "",
                "Bu belge OSYM uzerinden otomatik olusturulmustur (simulasyon)."
        ));
    }

    private void attach(Application app, String type, String fileName, List<String> lines) {
        try {
            Files.createDirectories(UPLOAD_DIRECTORY);
            String uniqueFileName = app.getApplicationId() + "_egov_" + type + "_" + fileName;
            Path filePath = UPLOAD_DIRECTORY.resolve(uniqueFileName).normalize();
            Files.write(filePath, buildPdf(lines),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            Document document = Document.builder()
                    .application(app)
                    .documentType(type)
                    .fileName(fileName)
                    .filePath(filePath.toString())
                    .ydyoApproved(false)
                    .documentUploadDate(LocalDate.now())
                    .active(true)
                    .build();
            documentRepository.save(document);
        } catch (Exception e) {
            // e-Devlet belgesi üretilemezse başvuru oluşturmayı bloklamayalım; logla, geç.
            System.err.println("e-Devlet belgesi üretilemedi (" + type + "): " + e.getMessage());
        }
    }

    // --- Yardımcılar ---

    private String buildFullName(Student s) {
        StringBuilder sb = new StringBuilder();
        if (s.getFirstName() != null) sb.append(s.getFirstName());
        if (s.getMiddleName() != null && !s.getMiddleName().isBlank()) sb.append(' ').append(s.getMiddleName());
        if (s.getLastName() != null) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(s.getLastName());
        }
        return sb.toString().trim();
    }

    private String safe(Object o) {
        return o == null ? "-" : String.valueOf(o);
    }

    // Türkçe karakterleri PDF (WinAnsi) ile uyumlu ASCII'ye çevirir.
    private String asciify(String s) {
        if (s == null) return "-";
        return s.replace('ş', 's').replace('Ş', 'S')
                .replace('ç', 'c').replace('Ç', 'C')
                .replace('ğ', 'g').replace('Ğ', 'G')
                .replace('ü', 'u').replace('Ü', 'U')
                .replace('ö', 'o').replace('Ö', 'O')
                .replace('ı', 'i').replace('İ', 'I')
                .replaceAll("[^\\x20-\\x7E]", "?");
    }

    // Bağımlılıksız, tek sayfalık geçerli bir PDF üretir (doğru xref offset'leriyle).
    private byte[] buildPdf(List<String> lines) {
        StringBuilder content = new StringBuilder();
        content.append("BT\n/F1 12 Tf\n72 770 Td\n16 TL\n");
        for (String line : lines) {
            content.append('(').append(escapePdf(line)).append(") Tj\nT*\n");
        }
        content.append("ET");
        byte[] contentBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);

        List<String> objects = new ArrayList<>();
        objects.add("<</Type /Catalog /Pages 2 0 R>>");
        objects.add("<</Type /Pages /Kids [3 0 R] /Count 1>>");
        objects.add("<</Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                + "/Resources <</Font <</F1 5 0 R>>>> /Contents 4 0 R>>");
        objects.add("<</Length " + contentBytes.length + ">>\nstream\n" + content + "\nendstream");
        objects.add("<</Type /Font /Subtype /Type1 /BaseFont /Helvetica>>");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeAscii(out, "%PDF-1.4\n");
        int[] offsets = new int[objects.size() + 1];
        for (int i = 0; i < objects.size(); i++) {
            offsets[i + 1] = out.size();
            writeAscii(out, (i + 1) + " 0 obj\n" + objects.get(i) + "\nendobj\n");
        }
        int xrefStart = out.size();
        writeAscii(out, "xref\n0 " + (objects.size() + 1) + "\n");
        writeAscii(out, "0000000000 65535 f \n");
        for (int i = 1; i <= objects.size(); i++) {
            writeAscii(out, String.format("%010d 00000 n \n", offsets[i]));
        }
        writeAscii(out, "trailer\n<</Size " + (objects.size() + 1) + " /Root 1 0 R>>\n"
                + "startxref\n" + xrefStart + "\n%%EOF");
        return out.toByteArray();
    }

    private void writeAscii(ByteArrayOutputStream out, String s) {
        byte[] b = s.getBytes(StandardCharsets.ISO_8859_1);
        out.write(b, 0, b.length);
    }

    private String escapePdf(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
