package com.pivotal.rabbitmq.gettingstarted;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class BenchmarkApplication {

	private static Logger log = LoggerFactory.getLogger(BenchmarkApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BenchmarkApplication.class, args);
	}


}
