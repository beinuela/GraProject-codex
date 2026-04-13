package com.campus.material;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.campus.material.modules")
@SpringBootApplication
public class CampusBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusBackendApplication.class, args);
    }
}
