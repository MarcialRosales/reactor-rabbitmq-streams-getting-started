package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.ReactiveRabbit;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.apache.avro.generic.GenericData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import schemas.event.Event;

import java.util.function.Consumer;

@Configuration
public class ConsumerProducerOfV1 extends BaseConsumerProducer {

    public static Logger log = LoggerFactory.getLogger(ConsumerProducerOfV1.class.getName());

    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "producer-specific-event-v1", matchIfMissing = false)
    public CommandLineRunner producerSpecificEventV1(Consumer<TopologyBuilder> eventTopology) {
        // @formatter:off
        return (args) -> {
            Flux<Event> events = Flux.range(1, 10).map(n -> Event
                    .newBuilder()
                    .setId(String.valueOf(n))
                    .setTimestamp(System.currentTimeMillis())
                    .build());

            rabbit.declareTopology(eventTopology)
                    .createProducerStream(Event.class)
                    .route().toExchange("events")
                    .then()
                    .send(events)
                    .doOnNext(e -> log.info("Sent {}", toString(e)))
                    .blockLast();
        };
        // @formatter:on
    }

    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "producer-generic-event-v1", matchIfMissing = false)
    public CommandLineRunner producerGenericEventV1(Consumer<TopologyBuilder> eventTopology) {
        // @formatter:off
        return (args) -> {
            Flux<GenericData.Record> events = Flux.range(1, 10).map(n -> {
                GenericData.Record record = reactiveRabbit.schemaManager().newRecord("schemas.event.Event");
                record.put("id", String.valueOf(n));
                record.put("timestamp", System.currentTimeMillis());
                return record;
            });

            rabbit.declareTopology(eventTopology)
                    .createProducerStream(GenericData.Record.class)
                    .route().toExchange("events")
                    .then()
                    .send(events)
                    .doOnNext(e -> log.info("Sent {}", toString(e)))
                    .blockLast();
        };
        // @formatter:on
    }
    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "consumer-specific-event-v1", matchIfMissing = false)
    public CommandLineRunner consumerSpecificEventV1(Consumer<TopologyBuilder> eventTopology) {
        // @formatter:off
        return (args) -> {
            rabbit.declareTopology(eventTopology)
                    .createTransactionalConsumerStream(queue, Event.class)
                    .whenReceiveIllegalEvents()
                        .alwaysReject()
                    .then()
                    .receive()
                    .transform(ReactiveRabbit.commitElseTerminate())
                    .subscribe(e -> log.info("Received {}", toString(e)));
        };
        // @formatter:on
    }

}
