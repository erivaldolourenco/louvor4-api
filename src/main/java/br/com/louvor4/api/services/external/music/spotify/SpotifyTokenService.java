package br.com.louvor4.api.services.external.music.spotify;

import br.com.louvor4.api.exceptions.ExternalMusicException;
import br.com.louvor4.api.services.external.music.spotify.dto.SpotifyTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class SpotifyTokenService {

    private static final Logger log = LoggerFactory.getLogger(SpotifyTokenService.class);
    private static final Duration EXPIRATION_MARGIN = Duration.ofSeconds(30);

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    private final RestClient spotifyAuthRestClient;

    private String accessToken;
    private Instant expiresAt = Instant.EPOCH;

    public SpotifyTokenService(RestClient spotifyAuthRestClient) {
        this.spotifyAuthRestClient = spotifyAuthRestClient;
    }

    public synchronized String getAccessToken() {
        if (accessToken == null || Instant.now().isAfter(expiresAt.minus(EXPIRATION_MARGIN))) {
            renewToken();
        }
        return accessToken;
    }

    private void renewToken() {
        String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        try {
            SpotifyTokenResponse response = spotifyAuthRestClient.post()
                    .uri("/api/token")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(SpotifyTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new ExternalMusicException("Resposta inválida ao obter token do Spotify.");
            }

            this.accessToken = response.accessToken();
            this.expiresAt = Instant.now().plusSeconds(response.expiresIn());
        } catch (RestClientException ex) {
            log.error("Falha ao renovar token do Spotify: {}", ex.getMessage(), ex);
            throw new ExternalMusicException("Falha ao autenticar no Spotify.", ex);
        }
    }
}
