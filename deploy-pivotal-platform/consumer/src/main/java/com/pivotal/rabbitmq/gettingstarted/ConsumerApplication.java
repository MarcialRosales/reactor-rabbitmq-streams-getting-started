package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.topology.TopologyBuilder;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ConsumerApplication {
    private static Logger log = LoggerFactory.getLogger(ConsumerApplication.class);
    @Value("${input:doublenumbers}")
    String input;

    public ConsumerApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }

    @Bean
    public Consumer<TopologyBuilder> topology() {
        return (builder) -> {
            builder.declareExchange(this.input)
                    .and()
                    .declareQueue(this.input)
                    .boundTo(this.input)
                        .withMaxLength(5);
        };
    }

    @Bean
    public Consumer<Integer> eventConsumer() {
        return (e) -> {
            log.info("Received {}", e);
        };
    }
}
