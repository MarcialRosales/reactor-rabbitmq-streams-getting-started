package com.pivotal.rabbitmq.gettingstarted;


import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.ReactiveRabbit;
import com.pivotal.rabbitmq.ReactiveRabbitBuilder;
import com.pivotal.rabbitmq.stream.ConsumerStream;
import com.pivotal.rabbitmq.stream.ProducerStream;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Send integers to a fanout exchange called numbers bound to a queue called numbers
 * And receive integers from queue called numbers
 */
public class BasicSendAndReceiveApplication {

	private static Logger log = LoggerFactory.getLogger(BasicSendAndReceiveApplication.class);

	public static void main(String[] args) {
		Optional<String> role = Arrays.stream(args).filter(option -> option.matches("--role=.*")).findFirst();
		if (!role.isPresent()) {
			help();
			System.exit(1);
		}
		String roleName = role.get().split("=")[1].trim();
		switch(roleName) {
			case "publisher":
				new BasicSendAndReceiveApplication().publisher();
				break;
			case "consumer":
				new BasicSendAndReceiveApplication().consumer();
				break;
			default:
				help();
		}

	}

	private static void help() {
		System.out.println("Syntax:");
		System.out.println("./run --role=publisher ");
		System.out.println("./run --role=consumer ");
	;
	}

	String EXCHANGE = "numbers";
	String QUEUE = "number";
	RabbitEndpointService rabbit;
	ReactiveRabbit reactiveRabbit;

	BasicSendAndReceiveApplication() {
		reactiveRabbit = ReactiveRabbitBuilder.newInstance()
			.forApplication("dummy")
			.withInstanceId("001")
			.addDefaultRabbitEndpoint()
			.build();

		rabbit = reactiveRabbit.selectDefaultEndpoint();
	}

	private Consumer<TopologyBuilder> topology() {
		// @formatter:off
		return (builder) -> builder
				.declareExchange(EXCHANGE)
				.and()
				.declareQueue(QUEUE)
					.boundTo(EXCHANGE)
					;
		// @formatter:on
	}

	public void consumer() {

		ConsumerStream<Integer> consumerStream = rabbit
				.declareTopology(topology())
				.createConsumerStream(QUEUE, Integer.class);

		consumerStream
				.receive()
				.doOnNext(number -> log.info("Received: {}", number))
				.subscribe();
	}

	public void publisher() {
		int count = 10;
		Duration delay = Duration.ofSeconds(1);

// @formatter:off
		Flux<Integer> integers = Flux
				.range(1, count)
				.delayElements(delay)
				.doOnNext(data -> log.debug("Sending: {}", data));

		ProducerStream<Integer> producerStream = rabbit
				.declareTopology(topology())
				.createProducerStream(Integer.class)
				.route()
					.toExchange(EXCHANGE)
				.then();

		producerStream
				.send(integers)
				.doOnNext(data -> log.debug("Sent: {}", data))
				.subscribe();
// @formatter:on
	}

}
