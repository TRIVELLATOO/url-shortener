package com.pablo.shortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // registra classes com @ConfigurationProperties, como AppProperties
public class ShortenerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShortenerApplication.class, args);
	}

}
