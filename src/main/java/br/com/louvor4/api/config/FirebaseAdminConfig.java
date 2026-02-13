package br.com.louvor4.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseAdminConfig {

    @Value("${firebase.credentials}")
    private String firebaseCredentialsJson;

    @PostConstruct
    public void init() throws IOException {
        try {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

//        InputStream serviceAccount =
//                new ClassPathResource("firebase-service-account.json").getInputStream();

            InputStream serviceAccount = new ByteArrayInputStream(
                    firebaseCredentialsJson.replace("\\n", "\n").getBytes(StandardCharsets.UTF_8)
            );

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);

        System.out.println("Firebase inicializado com sucesso!");

    } catch (IOException e) {
        throw new RuntimeException("Erro ao carregar as credenciais do Firebase no louvor4. " +
                "Verifique se a variável FIREBASE_JSON.", e);
    }
    }
}
