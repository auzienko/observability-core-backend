package com.auzienko.observability.corebackend.persistence;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.auzienko.observability.corebackend.persistence")
@EnableJpaRepositories(basePackages = "com.auzienko.observability.corebackend.persistence.repository")
@EntityScan(basePackages = "com.auzienko.observability.corebackend.persistence.entity")
public class PersistenceTestConfiguration {
}
