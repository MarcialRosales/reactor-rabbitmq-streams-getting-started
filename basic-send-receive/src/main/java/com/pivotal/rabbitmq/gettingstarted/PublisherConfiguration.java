package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.stream.ProducerStream;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.function.Consumer;

@Configuration
public class PublisherConfiguration {
    @Bean
    @ConditionalOnProperty(name = "role", havingValue = "publisher", matchIfMissing = false)
    public CommandLineRunner publisher(Consumer<TopologyBuilder> topology) {
        return (args) -> {
// @formatter:off
            Flux<Integer> integers = Flux
                    .range(1, count)
                    .delayElements(delay)
                    .doOnNext(data -> log.debug("Sending: {}", data));

            ProducerStream<Integer> producerStream = rabbit
                    .declareTopology(topology)
                    .createProducerStream(Integer.class)
                    .route()
                        .toExchange(NUMBERS)
                    .then();

            producerStream
                    .send(integers)
                    .doOnNext(data -> log.debug("Sent: {}", data))
                    .subscribe();
// @formatter:on
        };
    }


/*
                .whenUnroutable()
                    .alwaysRetry(Duration.ofSeconds(2))
                .then();

 */

    @Autowired
    RabbitEndpointService rabbit;

    @Value("${exchange:numbers}")
    String NUMBERS;

    @Value("${count:10}")
    int count;
    @Value("${delay:1s}")
    Duration delay;

    private static Logger log = LoggerFactory.getLogger(PublisherConfiguration.class);

}
