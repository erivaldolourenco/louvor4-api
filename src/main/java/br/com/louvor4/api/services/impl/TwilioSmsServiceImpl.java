package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.services.SmsService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "twilio")
public class TwilioSmsServiceImpl implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsServiceImpl.class);

    @Value("${app.sms.twilio.account-sid}")
    private String accountSid;

    @Value("${app.sms.twilio.auth-token}")
    private String authToken;

    @Value("${app.sms.twilio.from-number}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

    @Override
    public void sendPasswordResetCode(String to, String code) {
        String e164 = toE164Brazil(to);
        try {
            Message.creator(
                    new PhoneNumber(e164),
                    new PhoneNumber(fromNumber),
                    "Louvor4: seu código de recuperação é " + code + ". Expira em 15 minutos."
            ).create();
            log.info("SMS de recuperação enviado via Twilio para: {}", e164);
        } catch (Exception e) {
            log.error("Erro ao enviar SMS via Twilio para {}: {}", e164, e.getMessage());
            throw new RuntimeException("Não foi possível enviar o SMS. Tente pelo e-mail.");
        }
    }

    private String toE164Brazil(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if (phone.startsWith("+")) return "+" + digits;
        if (digits.startsWith("55")) return "+" + digits;
        return "+55" + digits;
    }
}
