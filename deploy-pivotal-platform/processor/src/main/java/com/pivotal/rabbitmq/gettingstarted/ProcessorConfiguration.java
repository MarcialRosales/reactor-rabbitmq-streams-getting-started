package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.ReactiveRabbit;
import com.pivotal.rabbitmq.stream.Transaction;
import com.pivotal.rabbitmq.stream.TransactionalProducerStream;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

@Configuration
public class ProcessorConfiguration {
    private static Logger log = LoggerFactory.getLogger(ProcessorConfiguration.class);
    @Autowired
    ReactiveRabbit reactiveRabbit;
    @Autowired
    RabbitEndpointService rabbit;

    public ProcessorConfiguration() {
    }

    @Bean
    public Disposable stream(Consumer<TopologyBuilder> topology,
                             @Value("${input:numbers}") String input,
                             @Value("${output:doublenumbers}") String exchange,
                             @Qualifier("multiplier") Function<Integer, Integer> processor) {
        assert processor != null;

        Flux<Transaction<Integer>> inputStream = this.rabbit
                .declareTopology(topology)
                .createTransactionalConsumerStream(input, Integer.class)
                .receive()
                .<Transaction<Integer>>handle((tx, sink) -> {
                    try {
                        sink.next(tx.map(processor.apply(tx.get())));
                    } catch (Throwable var4) {
                        tx.reject();
                    }
                }).doOnNext((tx) -> {
                    log.info("Producing {}", tx.get());
                });

        return this.rabbit
                .declareTopology(topology)
                .createTransactionalProducerStream(Integer.class)
                    .route().toExchange(exchange)
                .then()
                .send(inputStream).doOnNext((tx) -> log.info("Sent {}", tx.get()))
                .subscribe(Transaction::commit);
    }
}
