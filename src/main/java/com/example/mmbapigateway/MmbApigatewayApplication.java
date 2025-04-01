package com.example.mmbapigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MmbApigatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MmbApigatewayApplication.class, args);
    }

}
