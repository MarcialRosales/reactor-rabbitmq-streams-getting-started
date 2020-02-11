package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.RabbitEndpointService;
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
import java.util.function.Consumer;

import static com.pivotal.rabbitmq.topology.ExchangeType.fanout;

@SpringBootApplication
public class ReliableSendApplication {

	private static Logger log = LoggerFactory.getLogger(ReliableSendApplication.class);

	@Autowired
	RabbitEndpointService rabbit;

	public static void main(String[] args) {
		SpringApplication.run(ReliableSendApplication.class, args);
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "publisher", matchIfMissing = true)
	public CommandLineRunner help() {
		return (args) -> {
			System.out.println("Syntax:");
			System.out.println("./run --role=retryUnroutableEvents");
			System.out.println("./run --role=retryNackedEvents");
			System.out.println("./run --role=detourUnroutableEvents");

		} ;

	}

	Duration FIVE_SECONDS = Duration.ofSeconds(5);


	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "retryUnroutableEvents", matchIfMissing = false)
	public CommandLineRunner retryUnroutableEvents() {
		return (args) -> {


			// Build the stream that produces the data we will send later on
			// Our data consists of long numbers sent as String(s)
			Flux<Integer> streamOfDataToSend = Flux
					.range(1, 10)
					.delayElements(FIVE_SECONDS)
					.doOnNext(data -> log.debug("Sending: {}", data));
// @formatter:off
			rabbit
					.declareTopology((b) -> b.declareExchange("retryUnroutableEvents").type(fanout))
					.createProducerStream(Integer.class)
					.route()
						.toExchange("retryUnroutableEvents")
						.and()
					.whenUnroutable()
						.alwaysRetry(Duration.ofSeconds(5))
					.then()
					.send(streamOfDataToSend)
					.doOnNext(data -> log.debug("Sent: {}", data))
					.timeout(Duration.ofSeconds(FIVE_SECONDS.getSeconds()*10))
					.blockLast();
// @formatter:on

		};
	}

	private Consumer<TopologyBuilder> exchangeWithAlternate(String exchange, String ae) {
// @formatter:off
		return (b) -> b
				.declareExchange(ae)
					.type(fanout)
				.and()
				.declareQueue(ae)
					.boundTo(ae)
				.and()
				.declareExchange(exchange)
					.type(fanout)
					.withAlternateExchange(ae);
// @formatter:on
	}
	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "detourUnroutableEvents", matchIfMissing = false)
	public CommandLineRunner detourUnroutableEvents() {
		return (args) -> {


			// Build the stream that produces the data we will send later on
			// Our data consists of long numbers sent as String(s)
			Flux<Integer> streamOfDataToSend = Flux
					.range(1, 10)
					.delayElements(FIVE_SECONDS)
					.doOnNext(data -> log.debug("Sending: {}", data));
// @formatter:off
			String exchange = "detourUnroutableEvents";
			String ae = exchange+"-ae";
			rabbit
					.declareTopology(exchangeWithAlternate(exchange, ae))
					.createProducerStream(Integer.class)
					.route()
						.toExchange(exchange)
					.and()
					.whenUnroutable()
						.useAlternateExchange()
					.then()
					.send(streamOfDataToSend)
					.doOnNext(data -> log.debug("Sent: {}", data))
					.timeout(Duration.ofSeconds(FIVE_SECONDS.getSeconds()*10))
					.blockLast();
// @formatter:on

		};
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "retryNackedEvents", matchIfMissing = false)
	public CommandLineRunner retryNackedEvents() {
		return (args) -> {

			// Build the stream that produces the data we will send later on
			// Our data consists of long numbers sent as String(s)
			Flux<Integer> streamOfDataToSend = Flux
					.range(1, 10)
					.delayElements(FIVE_SECONDS)
					.doOnNext(data -> log.debug("Sending: {}", data));
// @formatter:off
			rabbit
					.declareTopology((b) -> b
						.declareExchange("retryNackedEvents")
							.type(fanout)
						.and()
						.declareQueue("retryNackedEvents")
							.boundTo("retryNackedEvents")
							.withMaxLength(1)
					)
					.createProducerStream(Integer.class)
					.route()
						.toExchange("retryNackedEvents")
						.and()
					.whenNackByBroker()
						.alwaysRetry(Duration.ofSeconds(2))
					.then()
					.send(streamOfDataToSend)
					.doOnNext(data -> log.debug("Sent: {}", data))
					.timeout(Duration.ofSeconds(FIVE_SECONDS.getSeconds()*10))
					.blockLast();
// @formatter:on
		};
	}



}
