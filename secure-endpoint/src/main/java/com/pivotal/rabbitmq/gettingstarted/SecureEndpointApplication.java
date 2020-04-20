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
public class SecureEndpointApplication {

	private static Logger log = LoggerFactory.getLogger(SecureEndpointApplication.class);

	@Autowired
	RabbitEndpointService rabbit;

	public static void main(String[] args) {
		SpringApplication.run(SecureEndpointApplication.class, args);
	}

	@Bean
	public CommandLineRunner run() {
		return (args) -> {
			int count = 1000;
			Duration senderDelay = Duration.ofMillis(250);

			Disposable.Composite pipelines = Disposables.composite();
			CountDownLatch waitForIntegers = new CountDownLatch(count);

// @formatter:off
			Flux<Integer> data = Flux.range(1, count).delayElements(senderDelay);
			pipelines.add(rabbit
					.declareTopology((b) -> b
							.declareExchange("secure-exchange")
							.type(fanout))
					.createProducerStream(Integer.class)
					.route()
						.toExchange("secure-exchange")
					.then()
					.send(data)
					.doOnNext(integer -> log.debug("Sent: {}", integer))
					.subscribe());

			pipelines.add(rabbit
					.declareTopology((b)->b
						.declareQueue("secure-queue")
							.classic()
								.withReplicas(1)
								.and()
							.boundTo("secure-exchange"))
					.createTransactionalConsumerStream("secure-queue", Integer.class)
					.receive()
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

	private Function<Flux<Transaction<Integer>>, Flux<Long>> logReceivedIntegersAfterDuration(Duration duration) {
		return (stream) -> {
			return stream.window(duration)
					.flatMap(Flux::count)
					.doOnNext(n -> log.debug("Received {} integers in last {}sec", n, duration.getSeconds()))
					.scan(Long::sum)
					.doOnNext(n -> log.debug("Received {} integers so far", n));

		};
	}


}
