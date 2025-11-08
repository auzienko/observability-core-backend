package com.auzienko.observability.corebackend.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.auzienko.observability.corebackend")
@EnableJpaRepositories(basePackages = "com.auzienko.observability.corebackend.persistence.repository")
@EntityScan(basePackages = "com.auzienko.observability.corebackend.persistence.entity")
@EnableAsync
@EnableScheduling
public class ObservabilityCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(ObservabilityCoreApplication.class, args);
    }

}
