package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.RabbitEndpointService;
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

import java.time.Duration;

import static com.pivotal.rabbitmq.topology.ExchangeType.fanout;

@SpringBootApplication
public class BasicSendAndReceiveApplication {

	private static Logger log = LoggerFactory.getLogger(BasicSendAndReceiveApplication.class);

	@Autowired
	RabbitEndpointService rabbit;

	public static void main(String[] args) {
		SpringApplication.run(BasicSendAndReceiveApplication.class, args);
	}

	@Value("${exchange:reactive-text}")
	String NUMBERS;
	@Value("${queue:reactive-text}")
	String CONSUMER;

	@Value("${count:10}")
	int count;
	@Value("${delay:10s}")
	Duration delay;


	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "help", matchIfMissing = true)
	public CommandLineRunner help() {
		return (args) -> {
			System.out.println("Syntax:");
			System.out.println("./run --role=publisher [--exchange=reactive-text] [--count=10] [--delay=10s]");
			System.out.println("./run --role=consumer [--queue=reactive-text] [--exchange=reactive-text]");
		} ;

	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "publisher", matchIfMissing = false)
	public CommandLineRunner publisher() {
		return (args) -> {

			// Build the stream that produces the data we will send later on
			// Our data consists of long numbers sent as String(s)
			Flux<Integer> streamOfDataToSend = Flux
					.range(1, count)
					.delayElements(delay)
					.doOnNext(data -> log.debug("Sending: {}", data));

			rabbit
					.declareTopology((b) -> b.declareExchange(NUMBERS).type(fanout))
					.createProducerStream(Integer.class)
					.route()
						.toExchange(NUMBERS)
					.then()
					.send(streamOfDataToSend)
					.doOnNext(data -> log.debug("Sent: {}", data))
					.blockLast();

		};
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "consumer", matchIfMissing = false)
	public CommandLineRunner consumer() {
		return (args) -> {

			rabbit
					.declareTopology((b) -> b
							.declareExchange(NUMBERS)
								.type(fanout)
							.and()
							.declareQueue(CONSUMER)
								.boundTo(NUMBERS)
					)
					.createConsumerStream(CONSUMER, Integer.class)
					.receive()
					.doOnNext(number -> log.info("Received: {}", number))
					.subscribe();
		};
	}

}
