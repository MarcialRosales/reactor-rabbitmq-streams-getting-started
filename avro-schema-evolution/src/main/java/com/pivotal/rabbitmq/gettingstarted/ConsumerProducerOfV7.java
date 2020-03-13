package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.ReactiveRabbit;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
public class ConsumerProducerOfV7 extends BaseConsumerProducer {

    public static Logger log = LoggerFactory.getLogger(ConsumerProducerOfV7.class.getName());

    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "producer-specific-event-v7", matchIfMissing = false)
    public CommandLineRunner producerSpecificEventV7(Consumer<TopologyBuilder> eventTopology) {

        return (args) -> {
            log.info("v7 is not forward compatible (with v1) => v1 cannot read v7");
            log.warn("v7 is not backward compatible (with v1) => v7 cannot read v1");
            Flux<schemas.event.v7.Event> events = Flux.range(1, 10).map(n -> schemas.event.v7.Event
                    .newBuilder()
                    .setNumber(n)
                    .build());

            rabbit.declareTopology(eventTopology)
                    .createProducerStream(schemas.event.v7.Event.class)
                    .route().toExchange("events")
                    .then()
                    .send(events)
                    .doOnNext(e -> log.info("Sent {}", toString(e)))
                    .blockLast();
        };
        // @formatter:on
    }
    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "consumer-specific-event-v7", matchIfMissing = false)
    public CommandLineRunner consumerSpecificEventV7(Consumer<TopologyBuilder> eventTopology) {

        log.info("v7 is not forward compatible (with v1) => v1 cannot read v7");
        log.warn("v7 is not backward compatible (with v1) => v7 cannot read v1");

        return (args) -> {
            rabbit.declareTopology(eventTopology)
                    .createTransactionalConsumerStream(queue, schemas.event.v7.Event.class)
                    .whenReceiveIllegalEvents()
                        .alwaysReject()
                    .then()
                    .receive()
                    .transform(ReactiveRabbit.commitElseTerminate())
                    .subscribe(e -> log.info("Received {}", toString(e)));
        };
    }

}
