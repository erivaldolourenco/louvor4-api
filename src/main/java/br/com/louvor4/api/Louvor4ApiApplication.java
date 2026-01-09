package br.com.louvor4.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@ConfigurationPropertiesScan
@SpringBootApplication
public class Louvor4ApiApplication {

    private static final Logger log = LoggerFactory.getLogger(Louvor4ApiApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(Louvor4ApiApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();

        String applicationName = env.getProperty("spring.application.name", "louvor4-api");
        String springVersion = SpringBootVersion.getVersion(); // Pega a versão do Spring Boot
        String serverPort = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");

        String protocol = env.getProperty("server.ssl.key-store") != null ? "https" : "http";
        String hostAddress = "localhost";

        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("Endereço host externo não encontrado.");
        }

        log.info("""
            
            ----------------------------------------------------------
            \tApplication '{}' is running!
            \tSpring Boot Version: v{}
            
            \tAccess URLs:
            \tLocal:      {}://localhost:{}{}
            \tExternal:   {}://{}:{}{}
            \tProfile(s): {}
            ----------------------------------------------------------
            """,
                applicationName,
                springVersion,
                protocol, serverPort, contextPath,
                protocol, hostAddress, serverPort, contextPath,
                env.getActiveProfiles().length == 0 ? "default" : String.join(", ", env.getActiveProfiles())
        );
    }
}
