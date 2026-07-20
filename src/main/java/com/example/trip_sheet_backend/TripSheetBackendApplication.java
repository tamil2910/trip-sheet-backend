package com.example.trip_sheet_backend;

import com.example.trip_sheet_backend.config.DotenvDefaultsLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TripSheetBackendApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(TripSheetBackendApplication.class);
		application.setDefaultProperties(DotenvDefaultsLoader.load());
		application.run(args);
	}

}
