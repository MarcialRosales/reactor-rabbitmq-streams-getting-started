package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.stream.Transaction;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.Disposable;

@Configuration
public class ConsumerConfiguration {
    @Autowired
    RabbitEndpointService rabbit;

    public ConsumerConfiguration() {
    }

    @Bean
    public Disposable stream(Consumer<TopologyBuilder> topology, @Value("${input:doublenumbers}") String queue, Consumer<Integer> consumer) {
        assert consumer != null;

        return rabbit
                .declareTopology(topology)
                .createTransactionalConsumerStream(queue, Integer.class)
                .receive()
                .<Transaction<Integer>>handle((tx, sink) -> {
                    try {
                        consumer.accept(tx.get());
                        sink.next(tx);
                    } catch (Throwable t) {
                        tx.reject();
                    }
                 }).subscribe(Transaction::commit);
    }
}
