package dev.queriva;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Queriva API gateway (search + ingest).
 */
@SpringBootApplication
public class QuerivaApplication {

    /**
     * Starts the Spring Boot application.
     */
    public static void main(String[] args) {
        SpringApplication.run(QuerivaApplication.class, args);
    }
}
