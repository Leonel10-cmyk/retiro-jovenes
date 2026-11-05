package com.retiro.jovenes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RetiroJovenesApplication {
    public static void main(String[] args) {
        SpringApplication.run(RetiroJovenesApplication.class, args);
    }
}
