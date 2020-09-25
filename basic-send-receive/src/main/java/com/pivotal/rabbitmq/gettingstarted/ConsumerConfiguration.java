package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.stream.ConsumerStream;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class ConsumerConfiguration {

    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "consumer", matchIfMissing = false)
    public CommandLineRunner consumer(Consumer<TopologyBuilder> topology) {
// @formatter:off
        return (args) -> {
            ConsumerStream<Integer> consumerStream = rabbit
                    .declareTopology(topology)
                    .createConsumerStream(consumerQueue, Integer.class);

            consumerStream
                    .receive()
                    .doOnNext(number -> log.info("Received: {}", number))
                    .subscribe();
        };
// @formatter:on
    }

    @Autowired
    RabbitEndpointService rabbit;

    @Value("${queue:numbers}")
    String consumerQueue;

    private static Logger log = LoggerFactory.getLogger(ConsumerConfiguration.class);
}
