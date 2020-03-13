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

import java.util.function.Consumer;

@Configuration
public class ConsumerProducerOfV6 extends BaseConsumerProducer {

    public static Logger log = LoggerFactory.getLogger(ConsumerProducerOfV6.class.getName());


    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "producer-specific-event-v6", matchIfMissing = false)
    public CommandLineRunner producerSpecificEventV6(Consumer<TopologyBuilder> eventTopology) {

        return (args) -> {
            log.info("v6 is forward compatible (with v1) => v1 can read v6");
            log.warn("v6 is backward compatible (with v1) => v6 can read v1");
            Flux<schemas.event.v6.Event> events = Flux.range(1, 10).map(n -> schemas.event.v6.Event
                    .newBuilder()
                    .setId(String.valueOf(n))
                    .setNumber(n*2)
                    .build());

            rabbit.declareTopology(eventTopology)
                    .createProducerStream(schemas.event.v6.Event.class)
                    .route().toExchange("events")
                    .then()
                    .send(events)
                    .doOnNext(e -> log.info("Sent {}", toString(e)))
                    .blockLast();
        };
        // @formatter:on
    }

    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "consumer-specific-event-v6", matchIfMissing = false)
    public CommandLineRunner consumerSpecificEventV6(Consumer<TopologyBuilder> eventTopology) {

        log.info("v6 is forward compatible (with v1) => v1 can read v6");
        log.warn("v6 is backward compatible (with v1) => v6 can read v1");

        return (args) -> {
            rabbit.declareTopology(eventTopology)
                    .createTransactionalConsumerStream(queue, schemas.event.v6.Event.class)
                    .whenReceiveIllegalEvents()
                        .alwaysReject()
                    .then()
                    .receive()
                    .transform(ReactiveRabbit.commitElseTerminate())
                    .subscribe(e -> log.info("Received {}", toString(e)));
        };
    }

    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "consumer-generic-event-v6", matchIfMissing = false)
    public CommandLineRunner consumerGenericEventV6(Consumer<TopologyBuilder> eventTopology) {

        log.info("v6 is forward compatible (with v1) => v1 can read v6");
        log.warn("v6 is backward compatible (with v1) => v6 can read v1");

        return (args) -> {
            rabbit.declareTopology(eventTopology)
                    .createTransactionalConsumerStream(queue, GenericData.Record.class)
                    .withAvroCodec()
                        .genericSchemaReaderFullName("schemas.event.v6.Event")
                    .and()
                    .whenReceiveIllegalEvents()
                        .alwaysReject()
                    .then()
                    .receive()
                    .transform(ReactiveRabbit.commitElseTerminate())
                    .subscribe(e -> log.info("Received {}", toString(e)));
        };
    }

}
