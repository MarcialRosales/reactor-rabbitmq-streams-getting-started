package com.pivotal.rabbitmq.gettingstarted;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.ReactiveRabbit;
import com.pivotal.rabbitmq.stream.Transaction;
import com.pivotal.rabbitmq.topology.MessageSelector;
import com.pivotal.rabbitmq.topology.Topology;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.apache.avro.generic.GenericData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import static com.pivotal.rabbitmq.topology.ExchangeType.*;
import static com.pivotal.rabbitmq.topology.Queue.MaxLengthStrategy.rejectPublishDlx;

@SpringBootApplication
public class ManagingTopologiesApplication {
	public static void main(String[] args) {
		SpringApplication.run(ManagingTopologiesApplication.class, args);
	}

	public static Logger log = LoggerFactory.getLogger(ManagingTopologiesApplication.class.getName());

	@Autowired
	RabbitEndpointService rabbit;

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "plain-simple", matchIfMissing = false)
	public CommandLineRunner createSimpleTopology() {
		// @formatter:off
		return (args) -> {

			log.info("Rabbit port\n{}", rabbit.getProperties().getPort());
			Topology provisionedTopology = rabbit
					.manageTopologies()
					.declare(simpleTopology("plain-simple"))
					.block();

			log.info("Topology provisioned\n{}", provisionedTopology);
		};
		// @formatter:on
	}

	Consumer<TopologyBuilder> simpleTopology(String name) {
		// @formatter:off
		return (builder) -> builder
						.declareExchange(name)
						.and()
						.declareQueue(name)
							.boundTo(name);
		// @formatter:on
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "topologyWithDeadLetterExchange", matchIfMissing = false)
	public CommandLineRunner topologyWithDeadLetterExchange() {
		// @formatter:off
		return (args) -> {
			Topology provisionedTopology = rabbit
					.manageTopologies()
					.declare(topologyWithDLX("dlx-demo"))
					.block();

			log.info("Topology provisioned\n{}", provisionedTopology);
		};
		// @formatter:on
	}

	private Consumer<TopologyBuilder> topologyWithDLX(String name) {
		return (builder) -> {
			// @formatter:off
			String dlx = name.concat("-DLX");
			builder
					.declareExchange(name)
					.and()
					.declareQueue(name)
						.boundTo(name)
						.withDeadLetterExchange(dlx)
						.withMaxLength(100, rejectPublishDlx)
					.and()
					.declareExchange(dlx)
					.and()
					.declareQueue(dlx)
						.boundTo(dlx);
			// @formatter:on
		};
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "topologyWithPartitions", matchIfMissing = false)
	public CommandLineRunner declareTopologyWithPartitions() {
		// @formatter:on
		return (args) -> {
			Topology provisionedTopology = rabbit
					.manageTopologies()
					.declare(topologyWithPartitions("partitions-demo"))
					.block();

			log.info("Topology provisioned\n{}", provisionedTopology);
		};
		// @formatter:on
	}

	private Consumer<TopologyBuilder> topologyWithPartitions(String name) {
		return (builder) -> {
			// @formatter:off
			builder
					.declareExchange(name)
					.and()
					.declareQueue(name)
						.boundTo(name)
						.withPartitions(2)
						.withPartitionKeyInMessageId();
			// @formatter:on
		};
	}

	@Autowired
	ReactiveRabbit reactiveRabbit;

	@Value("${cluster-endpoint:cluster}")
	String clusterEndpoint;

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "topologyWithReplicatedPartitionedQueue", matchIfMissing = false)
	public CommandLineRunner declareTopologyWithReplicatedPartitionedQueue() {
		// @formatter:off
		return (args) -> {
			Topology provisionedTopology = reactiveRabbit
				.selectEndpoint(clusterEndpoint)
				.manageTopologies()
				.declare(topologyWithReplicatedPartitionedQueue("replicated-and-partitioned"))
				.block();

			log.info("Topology provisioned\n{}", provisionedTopology);
		};
		// @formatter:on
	}
	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "topologyWithReplicatedPartitionedQueueUsingDefaultEndpoint", matchIfMissing = false)
	public CommandLineRunner declareTopologyWithReplicatedPartitionedQueueUsingDefaultEndpoint() {
		// @formatter:off
		return (args) -> {
			log.info("Use --spring.profiles.active=cluster to use RabbitMQ cluster");
			Topology provisionedTopology = rabbit
					.manageTopologies()
					.declare(topologyWithReplicatedPartitionedQueue("replicated-and-partitioned"))
					.block();

			log.info("Topology provisioned\n{}", provisionedTopology);
		};
		// @formatter:on
	}

	private Consumer<TopologyBuilder> topologyWithReplicatedPartitionedQueue(String name) {
		return (builder) -> {
			// @formatter:off
			builder
				.declareExchange(name)
				.and()
				.declareQueue(name)
					.boundTo(name)
					.withPartitions(2)
						.withPartitionKeyInMessageId()
					.and()
					.classic()
						.withReplicas(1, true)
			;
			// @formatter:on
		};
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "topologyWithFanoutToMultipleConsumers", matchIfMissing = false)
	public CommandLineRunner declareTopologyWithFanoutToMultipleConsumers() {
		// @formatter:off
		String name  = "fanout-to-multiple-consumers";
		return (args) -> {
			Topology provisionedTopology = rabbit
				.manageTopologies()
				.declare(topologyWithFanout(name)
						.andThen(subscriberOf(name, "consumer-1"))
						.andThen(subscriberOf(name, "consumer-2"))
						.andThen(subscriberOf(name, "consumer-3")))
				.block();

			log.info("Topology provisioned\n{}", provisionedTopology);
		};
		// @formatter:on
	}
	private Consumer<TopologyBuilder> subscriberOf(String exchange, String name) {
		return (builder) -> {
			builder
				.declareQueue(String.format("%s-%s", exchange, name))
				.boundTo(exchange);
		};
	}
	private Consumer<TopologyBuilder> topologyWithFanout(String name) {
		return (builder) -> {
			// @formatter:off
			builder
				.declareExchange(name)
				.type(fanout);
			// @formatter:on
		};
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "topicExchangeTopology", matchIfMissing = false)
	public CommandLineRunner declareTopicExchangeTopology() {
		// @formatter:off
		return (args) -> {
			Topology provisionedTopology = rabbit
				.manageTopologies()
				.declare(topicExchangeTopology("topic-exchange-topology"))
				.block();

			log.info("Topology provisioned\n{}", provisionedTopology);
		};
		// @formatter:on
	}

	private Consumer<TopologyBuilder> topicExchangeTopology(String name) {
		return (builder) -> {
			// @formatter:off
			builder
				.declareExchange(name)
					.type(topic)
				.and()
				.declareQueue(name.concat("-for-inventory"))
					.boundTo(name, "#.inventory")
				.and()
				.declareQueue(name.concat("-for-shipping"))
					.boundTo(name, "#.shipping")
			;
			// @formatter:on
		};
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "topologyWithAlternateExchange", matchIfMissing = false)
	public CommandLineRunner declareTopologyWithAlternateExchange() {
		// @formatter:off
		return (args) -> {
			Topology provisionedTopology = rabbit
				.manageTopologies()
				.declare(topologyWithAlternateExchange("ae-topology"))
				.block();

			log.info("Topology provisioned\n{}", provisionedTopology);
		};
		// @formatter:on
	}

	private Consumer<TopologyBuilder> topologyWithAlternateExchange(String name) {
		return (builder) -> {
			// @formatter:off
			String alternate = name.concat("-AE");
			builder
				.declareExchange(name)
					.withAlternateExchange(alternate)
				.and()
				.declareExchange(alternate)
				.and()
				.declareQueue(alternate)
					.boundTo(alternate)
			;
			// @formatter:on
		};
	}


	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "deleteTopology", matchIfMissing = false)
	public CommandLineRunner deleteTopology() {
		// @formatter:off
		return (args) -> {
			TopologyBuilder builder = reactiveRabbit.newTopologyBuilder();
			simpleTopology("plain-simple").accept(builder);
			rabbit
					.manageTopologies()
					.delete(builder.build())
					.block();
		};
		// @formatter:on
	}

	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "declareShipmentTopology", matchIfMissing = false)
	public CommandLineRunner declareShipmentTopology() {
		// @formatter:off
		return (args) -> {
			Topology provisionedTopology = rabbit
					.manageTopologies()
					.declare(shipmentTopology()
							.andThen(legalEventSubscriberTopology())
							.andThen(urgentTrainAnnouncementsSubscriberTopology())
							.andThen(auditSubscriberTopology())
							.andThen(carrierSubscriberTopology()))
					.block();

			log.info("Topology provisioned\n{}", provisionedTopology);
		};
		// @formatter:on
	}

	@Bean
	public Consumer<TopologyBuilder> shipmentTopology() {
		return (builder) -> {
// @formatter:off
			String shipments = "shipments";
			builder
					.declareExchange(shipments)
						.type(fanout)
					.and()
					.declareExchange(shipments.concat("-t"))
						.type(topic)
						.boundTo(shipments)
					.and()
					.declareExchange(shipments.concat("-h"))
						.type(headers)
						.boundTo(shipments)
					;
// @formatter:on
		};
	}
	@Bean
	public Consumer<TopologyBuilder> auditSubscriberTopology() {
		return (builder) -> {
// @formatter:off
			String subscriber = "shipment-audit";
			String shipments = "shipments";
			builder
					.declareQueue(subscriber)
					.boundTo(shipments)
					.withPartitions(2)
						.withPartitionKeyInMessageId()
			;
// @formatter:on
		};
	}
	Consumer<TopologyBuilder> legalEventSubscriberTopology() {
		return (builder) -> {
// @formatter:off
			String subscriber = "legal-events";
			String shipments = "shipments-t";
			builder
					.declareQueue(subscriber)
						.boundTo(shipments, "legal.#", "procurement.#")
			;
// @formatter:on
		};
	}
	Consumer<TopologyBuilder> urgentTrainAnnouncementsSubscriberTopology() {
		return (builder) -> {
// @formatter:off
			String subscriber = "urgent-train-announcements";
			String shipments = "shipments-h";
			builder
					.declareQueue(subscriber)
						.boundTo(shipments, MessageSelector
								.matchAll()
								.of("category_2", "urgent")
								.of("category_1", "announcement")
								.of("transport", "train"))
			;
// @formatter:on
		};
	}
	Consumer<TopologyBuilder> carrierSubscriberTopology() {
		return (builder) -> {
// @formatter:off
			String subscriber = "carrier";
			String subscriber_non_urgent = "carrier-non-urgent";
			String shipments = "shipments";
			builder
					.declareExchange(subscriber)
						.type(topic)
						.boundTo(shipments)
						.withAlternateExchange(subscriber_non_urgent)
					.and()
					.declareExchange(subscriber_non_urgent)
						.type(fanout)
					.and()
					.declareQueue(subscriber.concat("-urgent-shipments"))
						.boundTo(subscriber, "*.urgent.#")
					.and()
					.declareQueue(subscriber.concat("-non-urgent-shipments"))
						.boundTo(subscriber_non_urgent)
			;
// @formatter:on
		};
	}
	@Bean
	@ConditionalOnProperty(name = "role", havingValue = "auditor", matchIfMissing = false)
	public CommandLineRunner auditor(
			@Qualifier("auditSubscriberTopology")Consumer<TopologyBuilder> auditTopology) {
		return (args) -> {
			rabbit
					.declareTopology(auditTopology)
					.createTransactionalConsumerStream("shipment-audit", GenericData.Record.class)
					.receive()
					.doOnNext(txShipment -> log.info("Received shipment {} - {}.{}", txShipment.get().get("id"),
							txShipment.get().get("category_1"), txShipment.get().get("category_2")))
					.subscribe(Transaction::commit);

		};
	}

}

