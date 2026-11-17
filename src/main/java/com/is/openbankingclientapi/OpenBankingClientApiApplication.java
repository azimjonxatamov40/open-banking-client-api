package com.is.openbankingclientapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Ilovaning kirish nuqtasi.
// @SpringBootApplication — shu paketdagi (va ichki paketlardagi) barcha @Component, @Service,
// @RestController klasslarni topib, Spring konteyneriga yuklaydi va avtomatik sozlamalarni yoqadi
// (application.yml dagi datasource'dan JdbcTemplate, ichki Tomcat va h.k.).
@SpringBootApplication
public class  OpenBankingClientApiApplication {

    public static void main(String[] args) {
        // Spring kontekstini ko'taradi va ichki Tomcat serverni application.yml dagi portda (8080) ishga tushiradi
        SpringApplication.run(OpenBankingClientApiApplication.class, args);
    }

}
