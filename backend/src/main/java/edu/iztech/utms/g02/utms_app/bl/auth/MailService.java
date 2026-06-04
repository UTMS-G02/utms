package edu.iztech.utms.g02.utms_app.bl.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MailService {

    @Value("${resend.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendActivationEmail(String to, String firstName, String activationLink) {
        send(to, "UTMS - Hesabınızı Aktive Edin",
                "Merhaba " + firstName + ",\n\n" +
                "Hesabınızı aktive etmek için aşağıdaki bağlantıya tıklayın:\n" +
                activationLink + "\n\n" +
                "Bu bağlantı 24 saat boyunca geçerlidir.\n\n" +
                "Eğer bu isteği siz yapmadıysanız, bu e-postayı görmezden gelebilirsiniz.\n\n" +
                "UTMS Sistemi");
    }

    public void sendPasswordResetEmail(String to, String firstName, String resetLink) {
        send(to, "UTMS - Şifre Sıfırlama",
                "Merhaba " + firstName + ",\n\n" +
                "Şifrenizi sıfırlamak için aşağıdaki bağlantıya tıklayın:\n" +
                resetLink + "\n\n" +
                "Bu bağlantı 15 dakika boyunca geçerlidir.\n\n" +
                "Eğer bu isteği siz yapmadıysanız, bu e-postayı görmezden gelebilirsiniz.\n\n" +
                "UTMS Sistemi");
    }

    private void send(String to, String subject, String text) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("RESEND_API_KEY not set, skipping email to {}", to);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "from", "UTMS <onboarding@resend.dev>",
                "to", List.of(to),
                "subject", subject,
                "text", text
        );

        try {
            restTemplate.postForEntity(
                    "https://api.resend.com/emails",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            log.info("Email sent to {} via Resend", to);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}