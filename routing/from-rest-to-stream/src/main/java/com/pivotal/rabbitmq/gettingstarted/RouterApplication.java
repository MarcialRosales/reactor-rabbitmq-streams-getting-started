package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.ReactiveRabbit;
import com.pivotal.rabbitmq.gettingstarted.schemas.Shipment;
import com.pivotal.rabbitmq.source.OnDemandSource;
import com.pivotal.rabbitmq.source.Sender;
import com.pivotal.rabbitmq.source.Source;
import com.pivotal.rabbitmq.topology.ExchangeType;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.Disposable;
import reactor.util.function.Tuple2;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@SpringBootApplication
public class RouterApplication {
	public static void main(String[] args) {
		SpringApplication.run(RouterApplication.class, args);
	}

	@Autowired
	RabbitEndpointService rabbit;

	@Bean
	public Disposable router(RoutingTable routingTable, Source<Shipment> shipments) {
// @formatter:off
		// create the stream
		return rabbit
				.declareTopology(routingTable.topology())
				.createTransactionalProducerStream(Shipment.class)
				.routeWith((builder, shipment) -> builder
						.toExchange(RoutingTable.VOLUME_EXCHANGE)
						.withRoutingKey(routingTable.routingKey(shipment))
						.messageId(shipment.getId().toString())
						.header("category_1", shipment.getCategory1())
						.header("category_2", shipment.getCategory2())
				)
				.send(shipments.source())
				.transform(ReactiveRabbit
					.<Shipment>rejectWhen(RuntimeException.class)
					.terminateWhen(Throwable.class)
					.elseCommit()
				)
				.subscribe();
// @formatter:on

	}

	@Bean
	RoutingTable routingTable() {
		return new RoutingTable();
	}
	@Bean
	OnDemandSource<Shipment> shipments() {
		return new OnDemandSource<>("shipments");
	}
	@Bean
	Sender shipmentSender(OnDemandSource<Shipment> shipments) {
		return shipments;
	}

}

class RoutingTable {
	Map<String, Tuple2<Double, String>> routingTable = new HashMap<>();
	static String VOLUME_EXCHANGE = "volume";

	Function<Shipment, String> exchangeSelector() {
		return shipment -> VOLUME_EXCHANGE;
	}

	Function<Shipment, String> routingKeySelector() {
		return this::routingKey;
	}
	String routingKey(Shipment shipment) {
		if (shipment.getValue() <= 100000.0) return "10k";
		if (shipment.getValue() <= 500000.0) return "50k";
		else return "large";
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

