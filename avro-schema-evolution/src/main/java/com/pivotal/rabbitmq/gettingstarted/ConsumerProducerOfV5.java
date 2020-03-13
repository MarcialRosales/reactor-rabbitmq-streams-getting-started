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
public class ConsumerProducerOfV5 extends BaseConsumerProducer{

    public static Logger log = LoggerFactory.getLogger(ConsumerProducerOfV5.class.getName());


    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "producer-specific-event-v5", matchIfMissing = false)
    public CommandLineRunner producerSpecificEventV5(Consumer<TopologyBuilder> eventTopology) {
        // @formatter:off
        return (args) -> {
            log.info("v5 is forward compatible (with v1) => v1 can read v5");
            log.warn("v5 is NOT backward compatible (with v1) => v5 cannot read v1");
            Flux<schemas.event.v5.Event> events = Flux.range(1, 10).map(n -> schemas.event.v5.Event
                    .newBuilder()
                    .setId(String.valueOf(n))
                    .setNumber(n*2)
                    .build());

            rabbit.declareTopology(eventTopology)
                    .createProducerStream(schemas.event.v5.Event.class)
                    .route().toExchange("events")
                    .then()
                    .send(events)
                    .doOnNext(e -> log.info("Sent {}", toString(e)))
                    .blockLast();
        };
        // @formatter:on
    }
    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "consumer-specific-event-v5", matchIfMissing = false)
    public CommandLineRunner consumerSpecificEventV5(Consumer<TopologyBuilder> eventTopology) {

        log.info("v5 is forward compatible (with v1) => v1 can read v5");
        log.warn("v5 is NOT backward compatible (with v1) => v5 cannot read v1");
        return (args) -> {
            rabbit.declareTopology(eventTopology)
                    .createTransactionalConsumerStream(queue, schemas.event.v5.Event.class)
                    .whenReceiveIllegalEvents()
                        .alwaysReject()
                    .then()
                    .receive()
                    .transform(ReactiveRabbit.commitElseTerminate())
                    .subscribe(e -> log.info("Received {}", toString(e)));
        };
    }

}
