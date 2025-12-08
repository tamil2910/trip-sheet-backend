package com.example.trip_sheet_backend.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;

@Configuration
public class AppConfig {

  @Bean
  public ModelMapper modelMapper() {
      return new ModelMapper();
  }

  @Bean
  public Module hibernate6Module() {
      return new Hibernate6Module();
  }


}
