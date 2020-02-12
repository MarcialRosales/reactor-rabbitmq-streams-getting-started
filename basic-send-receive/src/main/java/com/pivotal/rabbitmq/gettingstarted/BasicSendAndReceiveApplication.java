package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.function.Consumer;

/**
 * Send integers to a fanout exchange called numbers bound to a queue called numbers
 * And receive integers from queue called numbers
 */
@SpringBootApplication
public class BasicSendAndReceiveApplication {

	private static Logger log = LoggerFactory.getLogger(BasicSendAndReceiveApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BasicSendAndReceiveApplication.class, args);
	}








	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "help", matchIfMissing = true)
	public CommandLineRunner help() {
		return (args) -> {
			System.out.println("Syntax:");
			System.out.println("./run --role=publisher [--exchange=reactive-text] [--count=10] [--delay=10s]");
			System.out.println("./run --role=consumer [--queue=reactive-text] [--exchange=reactive-text]");
		} ;
	}

	@Value("${exchange:numbers}")
	String NUMBERS;
	@Value("${queue:numbers}")
	String CONSUMER;

	@Bean
	public Consumer<TopologyBuilder> topology() {
		// @formatter:off
		return (builder) -> builder
				.declareExchange(NUMBERS)
				.and()
				.declareQueue(CONSUMER)
					.boundTo(NUMBERS)
					.withMaxLength(5);
		// @formatter:on
	}


}
