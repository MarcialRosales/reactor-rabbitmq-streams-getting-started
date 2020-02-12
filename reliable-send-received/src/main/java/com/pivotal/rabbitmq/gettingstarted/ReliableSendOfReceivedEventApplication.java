package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.stream.Transaction;
import com.pivotal.rabbitmq.stream.TransactionalConsumerStream;
import com.pivotal.rabbitmq.stream.TransactionalProducerStream;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

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

		public String exchange() {
			return name;
		}

		public String queue() {
			return name;
		}

	}

	class InputNumbers extends Numbers {
		public InputNumbers() {
			super("input-numbers");
		}
		Consumer<TopologyBuilder> forProducer() {
			return (topologyBuilder) -> topologyBuilder
					.declareExchange(name)
					;
		}
		Consumer<TopologyBuilder> forConsumer() {
			return (topologyBuilder) -> topologyBuilder
					.declareQueue(name)
						.boundTo(name)
						.withMaxLength(10)
					;
		}
	}
	class MultipliedNumbers extends Numbers {
		public MultipliedNumbers() {
			super("multiplied-numbers");
		}

		Consumer<TopologyBuilder> forProducer() {
			final String ae = name.concat("-ae");
			return topologyBuilder -> topologyBuilder
					.declareExchange(name)
						.withAlternateExchange(ae)
					.and()
					.declareExchange(ae)
					.and()
					.declareQueue(ae)
						.boundTo(ae);
		}
		Consumer<TopologyBuilder> forConsumer() {
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
	InputNumbers inputNumbersTopology = new InputNumbers();
	MultipliedNumbers multipliedNumbers = new MultipliedNumbers();

		@Bean
	@ConditionalOnProperty(name = "role", havingValue = "produceNumbers", matchIfMissing = false)
	public CommandLineRunner produceNumbers() {
		return (args) -> {

			Flux<Integer> streamOfNumbersToSend = Flux
				.range(1, 100);

// @formatter:off
			rabbit
				.declareTopology(inputNumbersTopology.forProducer())
				.createProducerStream(Integer.class)
				.route()
					.toExchange(inputNumbersTopology.exchange())
					.and()
					.whenUnroutable()
						.alwaysRetry(Duration.ofSeconds(2))
				.then()
				.send(streamOfNumbersToSend)
				.doOnNext(number -> log.info("Sent: {}", number))
				.blockLast();	// terminates when .range(1, count) emits the last value
// @formatter:on
		};
	}


	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "multiplier", matchIfMissing = false)
	public CommandLineRunner multiplier() {
		return (args) -> {

// @formatter:off
			// Receive input numbers
			TransactionalConsumerStream<Integer> transactionalStreamOfNumbers = rabbit
					.declareTopology(inputNumbersTopology.forConsumer())
					.createTransactionalConsumerStream(inputNumbersTopology.queue(), Integer.class)
					.withPrefetch(10)
					.ackEvery(5, Duration.ofSeconds(5));

			Flux<Transaction<Integer>> receivedNumbers = transactionalStreamOfNumbers.receive();

			// Multiply them
			Flux<Transaction<Long>> multipliedNumbers = receivedNumbers
				.map(txNumber -> txNumber.map(txNumber.get() * 2L));

			// And send
			TransactionalProducerStream<Long> streamOfMultipliedSentNumbers = rabbit
					.declareTopology(this.multipliedNumbers.forProducer())
					.createTransactionalProducerStream(Long.class)
					.route()
						.toExchange(this.multipliedNumbers.exchange())
					.then();

			Flux<Transaction<Long>> streamOfSentNumbers = streamOfMultipliedSentNumbers
				.send(multipliedNumbers)
				.doOnNext(n -> log.info("Sent multiplied number {}", n.get()))
				.delayElements(Duration.ofMillis(250));

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
				.declareTopology(multipliedNumbers.forConsumer())
				.createTransactionalConsumerStream(multipliedNumbers.queue(), Long.class)
				.receive()
				.transform(eliminateDuplicates())
				.doOnNext(txNumber -> log.info("Received {}", txNumber.get()))
				.subscribe(Transaction::commit);
// @formatter:on

		};
	}

	private Function<Flux<Transaction<Long>>, Flux<Transaction<Long>>> eliminateDuplicates() {
		Set<Long> receivedNumbers = new HashSet<>();
		return txNumbers -> txNumbers
				.filter(txNumber -> {
					boolean duplicate = !receivedNumbers.add(txNumber.get());
					if (duplicate) {
						log.warn("Found duplicate {}", txNumber.get());
					}
					return !duplicate;
				})
				.doOnDiscard(Transaction.class, Transaction::commit);
	}

}
