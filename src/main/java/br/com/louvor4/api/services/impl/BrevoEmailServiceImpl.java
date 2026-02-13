package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.EmailConfig;
import br.com.louvor4.api.services.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "brevo")
public class BrevoEmailServiceImpl implements EmailService {


    @Value("${app.email.brevo.api-key}")
    private String apiKey;

    @Value("${app.email.brevo.url}")
    private String apiUrl;

    private final EmailConfig emailConfig;
    private final RestTemplate restTemplate;

    public BrevoEmailServiceImpl(EmailConfig emailConfig) {
        this.emailConfig = emailConfig;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void sendPasswordResetCode(String to, String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey); // O Brevo pede a chave assim

        Map<String, Object> body = Map.of(
                "sender", Map.of("email", emailConfig.getFromEmail(), "name", "Louvor4"),
                "to", List.of(Map.of("email", to)),
                "subject", "Recuperação de Senha - Louvor4",
                "htmlContent", "<html><body><h1>Seu código é: <strong>" + code + "</strong></h1><p>O código expira em 15 minutos.</p></body></html>"
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("E-mail enviado com sucesso via Brevo para: " + to);
            }
        } catch (Exception e) {
            System.err.println("Erro ao enviar e-mail via Brevo: " + e.getMessage());
        }
    }
}
