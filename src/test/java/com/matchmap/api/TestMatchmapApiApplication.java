package com.matchmap.api;

import org.springframework.boot.SpringApplication;

public class TestMatchmapApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(MatchmapApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
