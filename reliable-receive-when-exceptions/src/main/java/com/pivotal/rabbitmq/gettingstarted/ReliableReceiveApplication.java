package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.ReactiveRabbit;
import com.pivotal.rabbitmq.stream.Transaction;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@SpringBootApplication
public class ReliableReceiveApplication {

	private static Logger log = LoggerFactory.getLogger(ReliableReceiveApplication.class);
	
	@Autowired
	RabbitEndpointService rabbit;

	public static void main(String[] args) {
		SpringApplication.run(ReliableReceiveApplication.class, args);
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "help", matchIfMissing = true)
	public CommandLineRunner help() {
		return (args) -> {
			System.out.println("Syntax:");
			System.out.println("./run --role=reliableConsumer");

		} ;
	}

	Duration FIVE_SECONDS = Duration.ofSeconds(5);

	private Consumer<TopologyBuilder> topology(String name) {
		return (topologyBuilder) -> topologyBuilder
				.declareExchange(name)
				.and()
				.declareQueue(name)
					.boundTo(name);
	}
	private Consumer<TopologyBuilder> topologyWithDeadLetterQueue(String name) {
		return (topologyBuilder) -> topologyBuilder
				.declareExchange(name)
				.and()
				.declareExchange(name + "-dlx")
				.and()
				.declareQueue(name + "-dlx")
					.boundTo(name + "-dlx")
				.and()
				.declareQueue(name)
					.withDeadLetterExchange(name + "-dlx")
					.boundTo(name);
	}
	private Flux<Integer> numbers(int count) {
		return Flux
				.range(1, count)
				.delayElements(FIVE_SECONDS)
				.doOnNext(data -> log.debug("Emitting: {}", data));
	}
	private Disposable sendNumbers(int count, Consumer<TopologyBuilder> topologyDeclarer, String exchangeName) {
		return rabbit
				.declareTopology(topologyDeclarer)
				.createProducerStream(Integer.class)
				.route().toExchange(exchangeName)
				.then()
				.send(numbers(count))
				.doOnNext(number -> log.debug("Sent: {}", number))
				.subscribe();
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "rejectOddNumbers", matchIfMissing = false)
	public CommandLineRunner rejectOddNumbers() {
		return (args) -> {

// @formatter:off
			String name = "rejectOddNumbers";
			CountDownLatch expectedMessage = new CountDownLatch(5);

			Disposable.Composite pipelines = Disposables.composite();
			// Publish numbers to the topology
			pipelines.add(sendNumbers(10, topology(name), name));

			// And consume numbers from the topology
			pipelines.add(rabbit
					.declareTopology(topology(name))
					.createTransactionalConsumerStream(name, Integer.class)
					.receive()
					.doOnNext(txNumber -> log.debug("Received: {}", txNumber.get()))
					.doOnNext(txNum -> {if (txNum.get() % 2 == 0) throw new RuntimeException();})
					.transform(ReactiveRabbit.<Integer>rejectWhen(RuntimeException.class).elseCommit())
					.doOnNext(txNumber -> log.debug("Processed: {}", txNumber.get()))
					.subscribe(txNumber -> {
						expectedMessage.countDown();
					})
			);

			if (expectedMessage.await(1, TimeUnit.MINUTES))
				log.info("Received the expected number of messages");
			pipelines.dispose();

// @formatter:on

		};
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "skipRejectAndTerminate", matchIfMissing = false)
	public CommandLineRunner skipRejectAndTerminate() {
		return (args) -> {

// @formatter:off
			String name = "skipRejectAndTerminate";
			int totalNumbers = 10;
			int rejected = 1;
			int terminated = 1;
			int filtered = 5;
			CountDownLatch expectedMessage = new CountDownLatch(totalNumbers-rejected-terminated-filtered);
			CountDownLatch expectedError = new CountDownLatch(1);

			Disposable.Composite pipelines = Disposables.composite();
			// Publish numbers to the topology
			pipelines.add(sendNumbers(totalNumbers, topologyWithDeadLetterQueue(name), name));

			// And consume numbers from the topology
			pipelines.add(rabbit
					.declareTopology(topologyWithDeadLetterQueue(name))
					.createTransactionalConsumerStream(name, Integer.class)
					.receive()
					.doOnNext(txNumber -> log.debug("Received: {}", txNumber.get()))
					.filter(txNumber -> txNumber.get() % 2 == 0)
					.doOnNext(txNum -> {
						if (txNum.get() == 4) throw new RuntimeException();
						else if (txNum.get() == 10) throw Exceptions.propagate(new Throwable());
					})
					//.doOnDiscard(Transaction.class, Transaction::commit) unnecessary once we have ReactiveRabbit.elseCommit
					.transform(ReactiveRabbit
							.<Integer>rejectWhen(RuntimeException.class)
							.terminateWhen(Throwable.class)
							.elseCommit())
					.doOnNext(txNumber -> log.debug("Processed: {}", txNumber.get()))
					.subscribe(txNumber -> {
						expectedMessage.countDown();
					}, throwable -> {
						log.error("Pipeline terminated", Exceptions.unwrap(throwable));
						expectedError.countDown();
					})
			);

			if (expectedMessage.await(1, TimeUnit.MINUTES))
				log.info("Received the expected number of messages. Checkout {}-dlx queue", name);
			if (expectedError.await(1, TimeUnit.MINUTES))
				log.info("Received the expected pipeline error");

			pipelines.dispose();

// @formatter:on

		};
	}


}
