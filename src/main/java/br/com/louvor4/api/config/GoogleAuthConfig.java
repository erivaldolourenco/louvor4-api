package br.com.louvor4.api.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class GoogleAuthConfig {

    @Value("${google.oauth.client-ids}")
    private String clientIdsRaw;

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier() {
        List<String> audience = Arrays.stream(clientIdsRaw.split(","))
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .toList();

        return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(audience)
                .build();
    }
}
