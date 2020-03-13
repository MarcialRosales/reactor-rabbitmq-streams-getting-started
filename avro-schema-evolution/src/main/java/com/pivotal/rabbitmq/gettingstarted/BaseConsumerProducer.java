package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.ReactiveRabbit;
import com.pivotal.rabbitmq.stream.Transaction;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

public abstract class BaseConsumerProducer {

    @Autowired
    RabbitEndpointService rabbit;

    @Value("${queue:events}")
    String queue;

    @Autowired
    ReactiveRabbit reactiveRabbit;

    protected String toString(Transaction<? extends GenericRecord> event) {
        return toString(event.get());
    }
    protected String toString(GenericRecord event) {
        StringBuilder sb = new StringBuilder("[");
        event.getSchema().getFields().forEach(f -> {
            Object value = event.get(f.name());
            sb.append(f.name()).append(":").append(value).append("; ");
        });
        sb.append("]");
        return sb.toString();
    }
}
