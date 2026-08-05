package com.adriandonaire.fooddeliveryops;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FoodDeliveryOpsApiApplication {

	public static void main(String[] args) {
		// Fijar zona horaria por defecto a UTC para evitar enviar IDs locales
		// que PostgreSQL podría no reconocer (ej. "America/Buenos_Aires").
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

		SpringApplication.run(FoodDeliveryOpsApiApplication.class, args);
	}

}
