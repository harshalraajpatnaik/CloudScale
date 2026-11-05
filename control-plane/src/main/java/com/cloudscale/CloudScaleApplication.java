package com.cloudscale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CloudScaleApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudScaleApplication.class, args);
    }
}
