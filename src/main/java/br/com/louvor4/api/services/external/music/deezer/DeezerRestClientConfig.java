package br.com.louvor4.api.services.external.music.deezer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class DeezerRestClientConfig {

    @Bean
    public RestClient deezerRestClient(ClientHttpRequestFactory externalApiClientHttpRequestFactory) {
        return RestClient.builder()
                .baseUrl("https://api.deezer.com")
                .requestFactory(externalApiClientHttpRequestFactory)
                .build();
    }
}
