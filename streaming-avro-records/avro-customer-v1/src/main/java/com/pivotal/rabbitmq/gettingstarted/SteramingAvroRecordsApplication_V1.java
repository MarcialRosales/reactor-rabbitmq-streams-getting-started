package com.pivotal.rabbitmq.gettingstarted;

import java.util.function.Consumer;

import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.ReactiveRabbit;
import com.pivotal.rabbitmq.gettingstarted.schemas.Customer;
import com.pivotal.rabbitmq.stream.ConsumerStream;
import com.pivotal.rabbitmq.stream.ProducerStream;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.apache.avro.generic.GenericData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SteramingAvroRecordsApplication_V1 {
	public static void main(String[] args) {
		SpringApplication.run(SteramingAvroRecordsApplication_V1.class, args);
	}

	public static Logger log = LoggerFactory.getLogger(SteramingAvroRecordsApplication_V1.class.getName());

	@Autowired
	RabbitEndpointService rabbit;

	@Autowired
	ReactiveRabbit reactiveRabbit;

	@Value("${topologyName:customers}")
	String topologyName;


	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "producerStreamOfCustomer", matchIfMissing = false)
	public CommandLineRunner producerStreamOfCustomer() {
		// @formatter:off
		return (args) -> {
			Flux<Customer> customersToSend = Flux
				.range(1, 10)
				.map(this::newCustomer);

			ProducerStream<Customer> customerProducerStream = rabbit
				.declareTopology(topology())
				.createProducerStream(Customer.class)
				.route()
					.toExchange(topologyName)
				.then();

			customerProducerStream
				.send(customersToSend)
				.doOnNext(sentCustomer -> log.info("Customer sent {}", sentCustomer))
				.blockLast();

		};
		// @formatter:on
	}

	private Customer newCustomer(Integer i) {
		Customer.Builder builder = Customer.newBuilder();
		builder.setId(String.valueOf(i));
		builder.setName("name-".concat(String.valueOf(i)));
		builder.setPhone((i % 2 == 0) ? String.valueOf(i).concat("494949") : String.valueOf(i).concat("885566"));
		return builder.build();
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "consumerStreamOfCustomer", matchIfMissing = false)
	public CommandLineRunner consumerStreamOfCustomer() {
		// @formatter:off
		return (args) -> {
			ConsumerStream<Customer> customerConsumerStream = rabbit
				.declareTopology(topology())
				.createConsumerStream(topologyName, Customer.class);

			customerConsumerStream
				.receive()
				.doOnNext(customer -> log.info("Customer received {}", customer.get("id")))
				.subscribe();
		};
		// @formatter:on
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "consumerStreamOfGenericRecord", matchIfMissing = false)
	public CommandLineRunner consumerStreamOfGenericRecord() {
		// @formatter:off
		return (args) -> {
			ConsumerStream<GenericData.Record> consumerStream = rabbit
				.declareTopology(topology())
				.createConsumerStream(topologyName, GenericData.Record.class);

			consumerStream
				.receive()
				.doOnNext(customer -> log.info("Generic customer received {}", customer.get("id")))
				.subscribe();
		};
		// @formatter:on
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "producerStreamOfGenericRecord", matchIfMissing = false)
	public CommandLineRunner producerStreamOfGenericRecord() {
		// @formatter:off
		return (args) -> {
			Flux<GenericData.Record> customersToSend = Flux.range(1, 10).map(this::newCustomerGeneric);
			String topologyName = "customers";
			rabbit
				.declareTopology(topology())
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

	Consumer<TopologyBuilder> topology() {
		return (topologyBuilder) -> {
			topologyBuilder
				.declareExchange(topologyName)
				.and()
				.declareQueue(topologyName)
					.boundTo(topologyName);
		};
	}

}

