package com.tih.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TihApp {

    public static void main(String[] args) {
        SpringApplication.run(TihApp.class, args);
    }
}
