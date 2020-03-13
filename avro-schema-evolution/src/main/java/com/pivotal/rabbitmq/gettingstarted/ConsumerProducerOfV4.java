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
public class ConsumerProducerOfV4 extends BaseConsumerProducer {

    public static Logger log = LoggerFactory.getLogger(ConsumerProducerOfV4.class.getName());



    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "producer-specific-event-v4", matchIfMissing = false)
    public CommandLineRunner producerSpecificEventV4(Consumer<TopologyBuilder> eventTopology) {
        // @formatter:off
        return (args) -> {
            log.info("v4 is forward compatible (with v1) => v1 can read v4");
            log.info("v4 is backward compatible (with v1) => v4 can read v1");
            Flux<schemas.event.v4.Event> events = Flux.range(1, 10).map(n -> schemas.event.v4.Event
                    .newBuilder()
                    .setId(String.valueOf(n))
                    .build());

            rabbit.declareTopology(eventTopology)
                    .createProducerStream(schemas.event.v4.Event.class)
                    .route().toExchange("events")
                    .then()
                    .send(events)
                    .doOnNext(e -> log.info("Sent {}", toString(e)))
                    .blockLast();
        };
        // @formatter:on
    }
    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "consumer-specific-event-v4", matchIfMissing = false)
    public CommandLineRunner consumerSpecificEventV4(Consumer<TopologyBuilder> eventTopology) {
        // @formatter:off
        log.info("v4 is backward compatible (with v1) => v4 can read v1");
        return (args) -> {
            rabbit.declareTopology(eventTopology)
                    .createTransactionalConsumerStream(queue, schemas.event.v4.Event.class)
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
