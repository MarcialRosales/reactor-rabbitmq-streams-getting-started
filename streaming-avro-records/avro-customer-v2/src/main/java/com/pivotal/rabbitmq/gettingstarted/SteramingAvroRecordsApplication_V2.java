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
public class SteramingAvroRecordsApplication_V2 {
	public static void main(String[] args) {
		SpringApplication.run(SteramingAvroRecordsApplication_V2.class, args);
	}

	public static Logger log = LoggerFactory.getLogger(SteramingAvroRecordsApplication_V2.class.getName());

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

	private Customer newCustomer(Integer i) {
		Customer.Builder builder = Customer.newBuilder();
		String id = String.valueOf(i);
		builder.setId(id);
		builder.setName("name-".concat(id));
		builder.setEmail("name-".concat(id.concat("@somewhere.com")));
		return builder.build();
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

