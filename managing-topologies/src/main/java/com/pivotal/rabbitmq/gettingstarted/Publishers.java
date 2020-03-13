package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.gettingstarted.schemas.Shipment;
import com.pivotal.rabbitmq.stream.Transaction;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.apache.avro.generic.GenericData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;
import java.util.function.Function;

import static com.pivotal.rabbitmq.gettingstarted.Publishers.ShipmentMetadata.*;

@Configuration
public class Publishers {
	static Logger log = LoggerFactory.getLogger(Publishers.class.getName());

	@Autowired
	RabbitEndpointService rabbit;

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "publishShipmentEvents", matchIfMissing = false)
	public CommandLineRunner publishShipmentEvents(
			@Qualifier("shipmentTopology")Consumer<TopologyBuilder> shipmentTopology) {
		return (args) -> {

			Shipment.Builder builder = Shipment.newBuilder();
			Flux<Shipment> shipments = Flux
					.just(builder
									.setId("1")
									.setCategory1("legal").build(),
							builder
									.setId("2")
									.setCategory1("legal")
									.setCategory2("urgent")
									.build(),
							builder
									.setId("3")
									.setCategory1("procurement")
									.setCategory2("normal")
									.build(),
							builder
									.setId("4")
									.setCategory1("announcement")
									.setCategory2("urgent")
									.setTransport("train")
									.build()
							);

			rabbit
					.declareTopology(shipmentTopology)
					.createProducerStream(Shipment.class)
					.route()
						.toExchange("shipments")
						.withRoutingKey(routingKey())
						.and()
					.withAttributes()
						.messageId(messageId())
						.header("category_1", category1())
						.header("category_2", category2())
					.then()
					.send(shipments)
					.doOnNext((data) -> log.info("Sent {}", data))
					.blockLast()
			;
		};
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

	}

}