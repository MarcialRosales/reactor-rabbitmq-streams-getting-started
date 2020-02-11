package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.stream.ProducerStream;
import com.pivotal.rabbitmq.stream.Transaction;
import com.pivotal.rabbitmq.stream.TransactionalConsumerStream;
import com.pivotal.rabbitmq.stream.TransactionalProducerStream;
import com.pivotal.rabbitmq.topologies.BroadcastTopology;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.function.Consumer;

@SpringBootApplication
public class ReliableSendOfReceivedEventApplication {

	private static final Logger log = LoggerFactory.getLogger(ReliableSendOfReceivedEventApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(ReliableSendOfReceivedEventApplication.class, args);
	}

	@Autowired
	RabbitEndpointService rabbit;

	class Numbers {
		String name;

		public Numbers(String name) {
			this.name = name;
		}

		private Consumer<TopologyBuilder> topology() {
			return (topologyBuilder) -> topologyBuilder
					.declareExchange(name)
					.and()
					.declareQueue(name)
						.boundTo(name)
						;
		}
	}
	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "help", matchIfMissing = true)
	public CommandLineRunner help() {
		return (args) -> {
			System.out.println("./run --role=produceNumbers");
			System.out.println("./run --role=receiveMultiplyAndSend");
			System.out.println("./run --role=receiveMultipliedNumbers");
		};
	}
	Numbers numbers = new Numbers("input-numbers");
	Numbers multipliedNumbers = new Numbers("multiplied-numbers");

		@Bean
	@ConditionalOnProperty(name = "role", havingValue = "produceNumbers", matchIfMissing = false)
	public CommandLineRunner produceNumbers() {
		return (args) -> {


			Flux<Integer> streamOfNumbersToSend = Flux
				.range(1, 100)
				//.delayElements(Duration.ofSeconds(1))
				.doOnNext(number -> log.info("Generated: {}", number));
// @formatter:off
			rabbit
				.declareTopology(numbers.topology())
				.createProducerStream(Integer.class)
				.route()
					.toExchange(numbers.name)
				.then()
				.send(streamOfNumbersToSend)
				.doOnNext(number -> log.info("Sent: {}", number))
				.blockLast();	// terminates when .range(1, count) emits the last value
// @formatter:on
		};
	}


	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "receiveMultiplyAndSend", matchIfMissing = false)
	public CommandLineRunner receiveMultiplyAndSend() {
		return (args) -> {

// @formatter:off

			Flux<Transaction<Integer>> streamOfReceivedNumbers = rabbit
				.declareTopology(numbers.topology())
				.createTransactionalConsumerStream(numbers.name, Integer.class)
				.receive();

			Flux<Transaction<Long>> streamOfMultipliedNumbers = streamOfReceivedNumbers
				.map(txNumber -> txNumber.map(txNumber.get() * 2L));

			Flux<Transaction<Long>> streamOfSentNumbers = rabbit
				.declareTopology(multipliedNumbers.topology())
				.createTransactionalProducerStream(Long.class)
				.route()
					.toExchange(multipliedNumbers.name)
				.then()
				.send(streamOfMultipliedNumbers);

			streamOfSentNumbers
				.subscribe(Transaction::commit);
// @formatter:on


		};
	}
	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "receiveMultipliedNumbers", matchIfMissing = false)
	public CommandLineRunner receiveMultipliedNumbers() {
		return (args) -> {

// @formatter:off
			rabbit
				.declareTopology(multipliedNumbers.topology())
				.createTransactionalConsumerStream(multipliedNumbers.name, Long.class)
				.receive()
				.doOnNext(txNumber -> log.info("Received {}", txNumber.get()))
				.subscribe(Transaction::commit);
// @formatter:on

		};
	}

}
