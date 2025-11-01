package com.adrian.blogweb1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Blogweb1Application {

	public static void main(String[] args) {
		SpringApplication.run(Blogweb1Application.class, args);
	}
	// Añade este método para que Spring gestione el RestTemplate
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
