package br.com.louvor4.api.services.external.music.spotify;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class SpotifyRestClientConfig {

    @Bean
    public RestClient spotifyAuthRestClient(ClientHttpRequestFactory externalApiClientHttpRequestFactory) {
        return RestClient.builder()
                .baseUrl("https://accounts.spotify.com")
                .requestFactory(externalApiClientHttpRequestFactory)
                .build();
    }

    @Bean
    public RestClient spotifyApiRestClient(ClientHttpRequestFactory externalApiClientHttpRequestFactory) {
        return RestClient.builder()
                .baseUrl("https://api.spotify.com/v1")
                .requestFactory(externalApiClientHttpRequestFactory)
                .build();
    }
}
