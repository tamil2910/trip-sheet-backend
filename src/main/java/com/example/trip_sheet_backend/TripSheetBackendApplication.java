package com.example.trip_sheet_backend;

import com.example.trip_sheet_backend.config.DotenvDefaultsLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TripSheetBackendApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(TripSheetBackendApplication.class);
		application.setDefaultProperties(DotenvDefaultsLoader.load());
		application.run(args);
	}

}

// mysql://root:dUWhVSBGtwyjrsuzhzpcglhywwsqTnOq@altaria.proxy.rlwy.net:47804/railway