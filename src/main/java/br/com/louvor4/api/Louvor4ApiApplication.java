package br.com.louvor4.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class Louvor4ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(Louvor4ApiApplication.class, args);
    }

}
