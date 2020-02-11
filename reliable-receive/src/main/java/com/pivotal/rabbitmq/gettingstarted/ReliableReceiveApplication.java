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
	@ConditionalOnProperty(name = "role", havingValue = "reliableConsumer", matchIfMissing = false)
	public CommandLineRunner reliableConsumer() {
		return (args) -> {

// @formatter:off
			String name = "reliableConsumer";
			CountDownLatch expectedMessage = new CountDownLatch(10);

			Disposable.Composite pipelines = Disposables.composite();
			// Publish numbers to the topology
			pipelines.add(sendNumbers(10, topology(name), name));

			// And consume numbers from the topology
			pipelines.add(rabbit
					.declareTopology(topology(name))
					.createTransactionalConsumerStream(name, Integer.class)
					.receive()
					.doOnNext(txNumber -> log.debug("Received: {}", txNumber.get()))
					.subscribe(txNumber -> {
						txNumber.commit();
						expectedMessage.countDown();
					})
			);

			expectedMessage.await(1, TimeUnit.MINUTES);
			pipelines.dispose();

// @formatter:on

		};
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "reliableSkippingConsumer", matchIfMissing = false)
	public CommandLineRunner reliableSkippingConsumer() {
		return (args) -> {

// @formatter:off
			String name = "reliableConsumer";
			int totalNumbers = 10;
			int evenNumbers = 5;
			CountDownLatch expectedMessage = new CountDownLatch(evenNumbers);

			Disposable.Composite pipelines = Disposables.composite();
			// Publish numbers to the topology
			pipelines.add(sendNumbers(totalNumbers, topology(name), name));

			// And consume numbers from the topology
			pipelines.add(rabbit
					.declareTopology(topology(name))
					.createTransactionalConsumerStream(name, Integer.class)
					.receive()
					.filter(txNumber -> txNumber.get().longValue() % 2 == 0)
					.doOnDiscard(Transaction.class, Transaction::commit)
					.doOnNext(txNumber -> log.debug("Received: {}", txNumber.get()))
					.subscribe(txNumber -> {
						txNumber.commit();
						expectedMessage.countDown();
					})
			);

			expectedMessage.await(1, TimeUnit.MINUTES);
			pipelines.dispose();

// @formatter:on

		};
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "abruptlyTerminatedConsumer", matchIfMissing = false)
	public CommandLineRunner abruptlyTerminatedConsumer() {
		return (args) -> {

// @formatter:off
			String name = "abruptlyTerminatedConsumer";
			int totalNumbers = 10;
			CountDownLatch terminated = new CountDownLatch(1);
			CountDownLatch expectedMessage = new CountDownLatch(totalNumbers);

			Disposable.Composite pipelines = Disposables.composite();
			// Publish numbers to the topology
			pipelines.add(sendNumbers(totalNumbers, topology(name), name));

			// And consume numbers from the topology
			pipelines.add(rabbit
					.declareTopology(topology(name))
					.createTransactionalConsumerStream(name, Integer.class)
					.withPrefetch(10)
					.receive()
					.doOnNext(txNumber -> log.debug("Received: {}", txNumber.get()))
					.delayElements(Duration.ofSeconds(20))
					.timeout(Duration.ofSeconds(20))
					.subscribe(txNumber -> {
						txNumber.commit();
						expectedMessage.countDown();
						if (expectedMessage.getCount() < 1) terminated.countDown();
					}, throwable -> {
							log.error("Receiver pipeline failed due to {}", throwable.getMessage());
							terminated.countDown();
						})
			);

			terminated.await(1, TimeUnit.MINUTES);
			log.info("Disposing all pipelines");
			pipelines.dispose();
			log.info("Disposed all pipelines");

// @formatter:on

		};
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "rejectIllegalEvent", matchIfMissing = false)
	public CommandLineRunner rejectIllegalEvent() {
		return (args) -> {

// @formatter:off
			String name = "rejectIllegalEvent";
			int totalNumbers = 10;
			CountDownLatch terminated = new CountDownLatch(1);
			CountDownLatch expectedMessage = new CountDownLatch(totalNumbers);

			Disposable.Composite pipelines = Disposables.composite();
			// Publish numbers to the topology
			pipelines.add(sendNumbers(totalNumbers, topologyWithDeadLetterQueue(name), name));

			// And consume numbers from the topology
			pipelines.add(rabbit
					.declareTopology(topologyWithDeadLetterQueue(name))
					.createTransactionalConsumerStream(name, String.class)
					.whenReceiveIllegalEvents()
						.alwaysReject() // rejected messages will go to the dead-letter-queue (default behaviour)
					.then()
					.receive()
					.doOnNext(txNumber -> log.debug("Received: {}", txNumber.get()))
					.doOnError(t -> log.error("Received {}", t.getMessage()))
					.subscribe(Transaction::commit)
			);
			log.info("Check rejectIllegalEvent-dlx for illegal events");

			terminated.await(1, TimeUnit.MINUTES);
			log.info("Disposing all pipelines");
			pipelines.dispose();
			log.info("Disposed all pipelines");

// @formatter:on

		};
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "terminateWhenIllegalEvent", matchIfMissing = false)
	public CommandLineRunner terminateWhenIllegalEvent() {
		return (args) -> {

// @formatter:off
			String name = "terminateWhenIllegalEvent";
			int totalNumbers = 10;
			CountDownLatch terminated = new CountDownLatch(1);
			CountDownLatch expectedMessage = new CountDownLatch(totalNumbers);

			Disposable.Composite pipelines = Disposables.composite();
			// Publish numbers to the topology
			pipelines.add(sendNumbers(totalNumbers, topology(name), name));

			// And consume numbers from the topology
			pipelines.add(rabbit
					.declareTopology(topology(name))
					.createTransactionalConsumerStream(name, String.class)
					.whenReceiveIllegalEvents()
						.alwaysTerminate()
					.then()
					.receive()
					.doOnNext(txNumber -> log.debug("Received: {}", txNumber.get()))
					.doOnError(t -> log.error("Received {}", t.getMessage()))
					.subscribe(Transaction::commit, t-> terminated.countDown())
			);

			terminated.await(1, TimeUnit.MINUTES);
			log.info("Disposing all pipelines");
			pipelines.dispose();
			log.info("Disposed all pipelines");

// @formatter:on

		};
	}


}
