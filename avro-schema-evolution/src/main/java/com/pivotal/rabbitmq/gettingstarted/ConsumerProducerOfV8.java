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
public class ConsumerProducerOfV8 extends BaseConsumerProducer {

    public static Logger log = LoggerFactory.getLogger(ConsumerProducerOfV8.class.getName());

    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "producer-specific-event-v8", matchIfMissing = false)
    public CommandLineRunner producerSpecificEventV8(Consumer<TopologyBuilder> eventTopology) {

        return (args) -> {
            Flux<schemas.event.v8.Event> events = Flux.range(1, 10).map(n -> schemas.event.v8.Event
                    .newBuilder()
                    .setNumber(n)
                    .build());

            rabbit.declareTopology(eventTopology)
                    .createProducerStream(schemas.event.v8.Event.class)
                    .route().toExchange("events")
                    .then()
                    .send(events)
                    .doOnNext(e -> log.info("Sent {}", toString(e)))
                    .blockLast();
        };
        // @formatter:on
    }
    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "consumer-specific-event-v8", matchIfMissing = false)
    public CommandLineRunner consumerSpecificEventV8(Consumer<TopologyBuilder> eventTopology) {

        return (args) -> {
            rabbit.declareTopology(eventTopology)
                    .createTransactionalConsumerStream(queue, schemas.event.v8.Event.class)
                    .whenReceiveIllegalEvents()
                        .alwaysReject()
                    .then()
                    .receive()
                    .transform(ReactiveRabbit.commitElseTerminate())
                    .subscribe(e -> log.info("Received {}", toString(e)));
        };
    }

}
