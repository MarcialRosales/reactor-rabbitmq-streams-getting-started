package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.stream.Transaction;
import com.pivotal.rabbitmq.topology.Topology;
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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

import static com.pivotal.rabbitmq.topology.ExchangeType.fanout;
import static java.time.Duration.ofSeconds;

@SpringBootApplication
public class ResilientEndpointApplication {

	private static Logger log = LoggerFactory.getLogger(ResilientEndpointApplication.class);

	@Autowired
	RabbitEndpointService rabbit;

	public static void main(String[] args) {
		SpringApplication.run(ResilientEndpointApplication.class, args);
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "resilient-producer-consumer", matchIfMissing = false)
	public CommandLineRunner resilientProducerConsumer() {
		return (args) -> {
			int count = 1000;
			Duration senderDelay = Duration.ofMillis(250);

			Disposable.Composite pipelines = Disposables.composite();
			CountDownLatch waitForIntegers = new CountDownLatch(count);

// @formatter:off
			Flux<Integer> data = Flux.range(1, count).delayElements(senderDelay);
			pipelines.add(rabbit
					.declareTopology((b) -> b
							.declareExchange("resilient-producer")
							.type(fanout))
					.createProducerStream(Integer.class)
					.route()
						.toExchange("resilient-producer")
					.then()
					.send(data)
					.doOnNext(integer -> log.debug("Sent: {}", integer))
					.subscribe());

			pipelines.add(rabbit
					.declareTopology((b)->b
						.declareQueue("resilient-consumer")
							.classic()
								.withReplicas(1)
								.and()
							.boundTo("resilient-producer"))
					.createTransactionalConsumerStream("resilient-consumer", Integer.class)
					.receive()
					.transform(eliminateDuplicates())
					.doOnNext(Transaction::commit)
					.doOnNext(txInt -> waitForIntegers.countDown())
					.transform(logReceivedIntegersAfterDuration(ofSeconds(5)))
					.subscribe());
// @formatter:on

			waitForIntegers.await();
			log.info("Received all {} integers", count);
			pipelines.dispose();
		};
	}
	private Function<Flux<Transaction<Integer>>, Flux<Transaction<Integer>>> eliminateDuplicates() {
		return (stream) -> {
			Set<Integer> receivedIntegers = new HashSet<>();
			return stream
					.filter(txInt -> receivedIntegers.add(txInt.get()))
					.doOnDiscard(Transaction.class, Transaction::commit);
		};
	}
	private Function<Flux<Transaction<Integer>>, Flux<Long>> logReceivedIntegersAfterDuration(Duration duration) {
		return (stream) -> {
			return stream.window(duration)
					.flatMap(Flux::count)
					.doOnNext(n -> log.debug("Received {} integers in last {}sec", n, duration.getSeconds()))
					.scan(Long::sum)
					.doOnNext(n -> log.debug("Received {} integers so far", n));

		};
	}

		@Bean
	@ConditionalOnProperty(name = "role", havingValue = "resilient-declare", matchIfMissing = false)
	public CommandLineRunner createSimpleTopology() {
		// @formatter:off
		return (args) -> {

			log.info("Rabbit port\n{}", rabbit.getProperties().getPort());
			Topology provisionedTopology = rabbit
					.manageTopologies()
					.declare(b->b.declareExchange("resilient-declare")
							.withAlternateExchange("resilient-declare-ae")) // to force a policy creation
					.block();

			log.info("Topology provisioned\n{}", provisionedTopology);
		};
		// @formatter:on
	}

}
