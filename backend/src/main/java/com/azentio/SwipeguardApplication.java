package com.azentio.swipeguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SwipeguardApplication {
    public static void main(String[] args) {
        SpringApplication.run(SwipeguardApplication.class, args);
    }
}