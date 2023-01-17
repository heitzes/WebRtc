package com.example.signalling2;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;


@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class Signalling2Application {

    public static void main(String[] args) {
        SpringApplication.run(Signalling2Application.class, args);
    }

}
