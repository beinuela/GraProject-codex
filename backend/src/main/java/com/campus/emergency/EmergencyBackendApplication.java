package com.campus.emergency;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.campus.emergency.modules")
@SpringBootApplication
public class EmergencyBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmergencyBackendApplication.class, args);
    }
}
