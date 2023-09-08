package br.com.louvor4.louvor4api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("louvor4-api")
                        .version("v1")
                        .description("Api Aplicativo louvor4")
                        .termsOfService("")
                        .license(
                                new License()
                                        .name("Apache2.0")
                                        .url(""))
                );
    }
}
