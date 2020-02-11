package com.pivotal.rabbitmq.gettingstarted;

import java.util.ArrayList;
import java.util.List;

import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.ReactiveRabbit;
import com.pivotal.rabbitmq.gettingstarted.schemas.Customer;
import com.pivotal.rabbitmq.schema.SchemaManager;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SteramingAvroRecordsApplication {
	public static void main(String[] args) {
		SpringApplication.run(SteramingAvroRecordsApplication.class, args);
	}

	public static Logger log = LoggerFactory.getLogger(SteramingAvroRecordsApplication.class.getName());

	@Autowired
	RabbitEndpointService rabbit;

	@Autowired
	ReactiveRabbit reactiveRabbit;

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "producerStreamOfGenericRecord", matchIfMissing = false)
	public CommandLineRunner producerStreamOfGenericRecord() {
		// @formatter:off
		return (args) -> {
			Flux<GenericData.Record> customersToSend = Flux.range(1, 10).map(this::newCustomerGeneric);
			String topologyName = "customers-generic";
			rabbit
				.declareTopology(b -> b.declareExchange(topologyName))
				.createProducerStream(GenericData.Record.class)
					.route()
					.toExchange(topologyName)
				.then()
				.send(customersToSend)
				.doOnNext(sentCustomer -> log.info("Generic customer sent {}", sentCustomer))
				.blockLast();

		};
		// @formatter:on
	}

	private GenericData.Record newCustomerGeneric(Integer i) {
		GenericData.Record record = reactiveRabbit
			.schemaManager()
			.newRecord("Customer");

		record.put("id", String.valueOf(i));
		record.put("name", "name-".concat(String.valueOf(i)));
		record.put("phone", i.intValue() % 2 == 0 ? "848484" : "");
		return record;
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "consumerStreamOfGenericRecord", matchIfMissing = false)
	public CommandLineRunner consumerStreamOfGenericRecord() {
		// @formatter:off
		return (args) -> {
			String topologyName = "customers-generic";
			rabbit
				.declareTopology(b -> b.declareQueue(topologyName).boundTo(topologyName))
				.createConsumerStream(topologyName, GenericData.Record.class)
				.receive()
				.doOnNext(customer -> log.info("Generic customer received {}", customer.get("id")))
				.subscribe();
		};
		// @formatter:on
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "producerStreamOfSpecificRecord", matchIfMissing = false)
	public CommandLineRunner producerStreamOfSpecificRecord() {
		// @formatter:off
		return (args) -> {
			Flux<Customer> customersToSend = Flux.range(1, 10).map(this::newCustomerSpecific);
			String topologyName = "customers-generic";
			rabbit
				.declareTopology(b -> b.declareExchange(topologyName))
				.createProducerStream(Customer.class)
				.route()
				.toExchange(topologyName)
				.then()
				.send(customersToSend)
				.doOnNext(sentCustomer -> log.info("Specific customer sent {}", sentCustomer))
				.blockLast();

		};
		// @formatter:on
	}

	private Customer newCustomerSpecific(Integer i) {
		Customer.Builder builder = Customer.newBuilder();
		builder.setId(String.valueOf(i));
		builder.setName("name-".concat(String.valueOf(i)));
		builder.setPhone(i.intValue() % 2 == 0 ? "99939" : "");
		return builder.build();
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "consumerStreamOfSpecificRecord", matchIfMissing = false)
	public CommandLineRunner consumerStreamOfSpecificRecord() {
		// @formatter:off
		return (args) -> {
			String topologyName = "customers-generic";
			rabbit
				.declareTopology(b -> b.declareQueue(topologyName).boundTo(topologyName))
				.createConsumerStream(topologyName, Customer.class)
				.receive()
				.doOnNext(customer -> log.info("Specific customer received {}", customer.get("id")))
				.subscribe();
		};
		// @formatter:on
	}

}

