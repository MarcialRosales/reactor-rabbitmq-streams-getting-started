package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.ReactiveRabbit;
import com.pivotal.rabbitmq.gettingstarted.schemas.Shipment;
import com.pivotal.rabbitmq.topology.ExchangeType;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.pivotal.rabbitmq.gettingstarted.RouterApplication.ShipmentMetadata.*;

@SpringBootApplication
public class RouterApplication {
	public static void main(String[] args) {
		SpringApplication.run(RouterApplication.class, args);
	}

	public static Logger log = LoggerFactory.getLogger(RouterApplication.class.getName());

	@Autowired
	RabbitEndpointService rabbit;

	@Autowired
	ReactiveRabbit reactiveRabbit;

	@Bean
	public Disposable router(RoutingTable routingTable, Flux<Shipment> shipments) {
		// @formatter:off
		return rabbit
					.declareTopology(routingTable.topology())
					.createProducerStream(Shipment.class)
					.withAttributes()	// Tag event with attributes; maybe someone might need it
						.messageId(messageId())
						.header("category_1", category1())
						.header("category_2", category2())
					.and()
					.route()	// Dynamic Routing based on business rules
						.toExchange(routingTable.VOLUME_EXCHANGE)
						.withRoutingKey(routingTable.routingKeySelector())
					.then()		// We can skip/drop shipments we do not need to send based on some criteria
					.send(shipments.filter(skipShipmentWithUnknownTransport()))
					.subscribe();
		// @formatter:on
	}


	@Bean
	RoutingTable routingTable() {
		return new RoutingTable();
	}

	@Bean
	Flux<Shipment> shipments() {
		Shipment.Builder builder = Shipment.newBuilder();
		long prefix = System.currentTimeMillis();
		Random random = new Random(System.currentTimeMillis());

		return Flux.range(1, 50000)
				.map(i -> builder
						.setId(String.format("%d:%d", prefix, i))
						.setCategory1("unknown")
						.setValue(random.nextInt(100)*10000+random.nextInt(1000))
						.build()
				);
	}
	static class ShipmentMetadata {
		public static Function<Shipment, String> routingKey() {
			return (s) -> String.format("%s.%s", s.getCategory1(), s.getCategory2());
		}
		public static Function<Shipment, String> messageId() {
			return (s) -> s.getId().toString();
		}
		public static Function<Shipment, String> category1() {
			return (s) -> s.getCategory1().toString();
		}
		public static Function<Shipment, String> category2() {
			return (s) -> s.getCategory2().toString();
		}
		public static Predicate<Shipment> skipShipmentWithUnknownTransport() {
			return s -> !s.getTransport().equals("unknown");
		}
	}
}

/**
 * Route shipments based on value to the "volume" direct exchange.
 *
 * Shipments Up to 10k are sent to 10k queue
 * Shipments Up to 50k are sent to 50k queue
 * Shipments greater than 50k are sent to large queue
 *
 */
class RoutingTable {
	Map<String, Tuple2<Double, String>> routingTable = new HashMap<>();
	String VOLUME_EXCHANGE = "volume";

	Function<Shipment, String> exchangeSelector() {
		return shipment -> VOLUME_EXCHANGE;
	}

	Function<Shipment, String> routingKeySelector() {
		return shipment -> {
			if (shipment.getValue() <= 100000.0) return "10k";
			if (shipment.getValue() <= 500000.0) return "50k";
			else return "large";
		};
	}

	Consumer<TopologyBuilder> topology() {
		// @formatter:off
		return builder -> builder
				.declareExchange("volume")
					.type(ExchangeType.direct)
				.and()
				.declareQueue("10k")
					.boundTo("volume", "10k")
				.and()
				.declareQueue("50k")
					.boundTo("volume", "50k")
				.and()
				.declareQueue("large")
					.boundTo("volume", "large");
		// @formatter:on

	}
}

