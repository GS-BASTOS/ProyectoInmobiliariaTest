package com.inmobiliaria.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class InmobiliariaAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(InmobiliariaAppApplication.class, args);
    }
}
