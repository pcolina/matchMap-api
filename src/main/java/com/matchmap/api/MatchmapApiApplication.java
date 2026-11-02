package com.matchmap.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MatchmapApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(MatchmapApiApplication.class, args);
  }
}
