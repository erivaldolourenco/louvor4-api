package br.com.louvor4.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Aplica para todas as rotas
                        .allowedOrigins("http://localhost:4200") // Origem do frontend Angular
                        .allowedOrigins("http://192.168.0.108:4200") // Origem do frontend Angular
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Métodos permitidos
                        .allowedHeaders("*") // Todos os headers
                        .allowCredentials(true); // Permitir envio de cookies e headers de auth
            }
        };
    }
}
