package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.function.Consumer;

@SpringBootApplication
public class StreamingAvroRecordsApplication {
	public static void main(String[] args) {
		SpringApplication.run(StreamingAvroRecordsApplication.class, args);
	}

	public static Logger log = LoggerFactory.getLogger(StreamingAvroRecordsApplication.class.getName());

	@Bean
	public Consumer<TopologyBuilder> eventTopology() {
// @formatter:off
		return b -> b
				.declareExchange("events")
				.and()
				.declareQueue("events")
				.boundTo("events")
				.withDeadLetterExchange("events-dlx")
				.and()
				.declareExchange("events-dlx")
				.and()
				.declareQueue("events-dlq")
				.boundTo("events-dlx");
// @formatter:on

	}
}
