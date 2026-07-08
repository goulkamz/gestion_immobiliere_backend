package com.immobilier.gestionImmobiliere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GestionImmobiliereApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestionImmobiliereApplication.class, args);
	}

}
