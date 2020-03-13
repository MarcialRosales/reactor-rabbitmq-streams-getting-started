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

import java.util.function.Consumer;

@Configuration
public class GenericConsumer extends BaseConsumerProducer {

    public static Logger log = LoggerFactory.getLogger(GenericConsumer.class.getName());


    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "consumer-generic-event", matchIfMissing = false)
    public CommandLineRunner consumerGenericEvent(Consumer<TopologyBuilder> eventTopology) {
        // @formatter:off
        return (args) -> {
            rabbit.declareTopology(eventTopology)
                    .createTransactionalConsumerStream(queue, GenericData.Record.class)
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
