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
public class ConsumerProducerOfV2 extends BaseConsumerProducer {

    public static Logger log = LoggerFactory.getLogger(ConsumerProducerOfV2.class.getName());

    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "consumer-specific-event-v2", matchIfMissing = false)
    public CommandLineRunner consumerSpecificEventV2(Consumer<TopologyBuilder> eventTopology) {
        // @formatter:off
        log.warn("v2 is not backward compatible (with v1)");
        return (args) -> {
            rabbit.declareTopology(eventTopology)
                    .createTransactionalConsumerStream(queue, schemas.event.v2.Event.class)
                    .whenReceiveIllegalEvents()
                        .alwaysReject()
                    .then()
                    .receive()
                    .transform(ReactiveRabbit.commitElseTerminate())
                    .subscribe(e -> log.info("Received {}", toString(e)));
        };
        // @formatter:on
    }
    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "producer-specific-event-v2", matchIfMissing = false)
    public CommandLineRunner producerSpecificEventV2(Consumer<TopologyBuilder> eventTopology) {
        // @formatter:off
        return (args) -> {
            log.info("v2 is forward compatible (with v1)");
            Flux<schemas.event.v2.Event> events = Flux.range(1, 10).map(n -> schemas.event.v2.Event
                    .newBuilder()
                    .setId(String.valueOf(n))
                    .setTimestamp(System.currentTimeMillis())
                    .setNumber(n*2)
                    .build());

            rabbit.declareTopology(eventTopology)
                    .createProducerStream(schemas.event.v2.Event.class)
                    .route().toExchange("events")
                    .then()
                    .send(events)
                    .doOnNext(e -> log.info("Sent {}", toString(e)))
                    .blockLast();
        };
        // @formatter:on
    }

}
