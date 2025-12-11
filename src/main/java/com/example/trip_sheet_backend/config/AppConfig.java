package com.example.trip_sheet_backend.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import com.example.trip_sheet_backend.models.Vehicle;
import com.example.trip_sheet_backend.dtos.DriverInfoDto;
import com.example.trip_sheet_backend.dtos.VehicleInfoDto;
import com.example.trip_sheet_backend.models.Driver;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;

@Configuration
public class AppConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        // Strict mode prevents wrong field mapping
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        // VEHICLE mappings
        mapper.typeMap(VehicleInfoDto.class, Vehicle.class)
              .addMappings(m -> {
                  m.skip(Vehicle::setId);
                  m.skip(Vehicle::setTenant);
                  m.skip(Vehicle::setVehicleType);
                  m.skip(Vehicle::setCreatedBy);
              });

        // DRIVER mappings
        mapper.typeMap(DriverInfoDto.class, Driver.class)
              .addMappings(m -> {
                  m.skip(Driver::setId);
                  m.skip(Driver::setAccount);
                  m.skip(Driver::setCreatedBy);
              });

        return mapper;
    }

    @Bean
    public Module hibernate6Module() {
        return new Hibernate6Module();
    }
}
