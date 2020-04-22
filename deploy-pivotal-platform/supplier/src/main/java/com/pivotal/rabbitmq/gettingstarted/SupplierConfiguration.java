package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.stream.ProducerStream;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

@Configuration
public class SupplierConfiguration {
    @Autowired
    RabbitEndpointService rabbit;

    public SupplierConfiguration() {
    }

    @Bean
    public Disposable stream(Consumer<TopologyBuilder> topology,
                             @Value("${output:numbers}") String exchange,
                             Supplier<Flux<Integer>> supplier) {
        return this.rabbit
                .declareTopology(topology)
                .createProducerStream(Integer.class)
                .route()
                    .toExchange(exchange)
                .then()
                .send(supplier.get())
                .subscribe();
    }
}
