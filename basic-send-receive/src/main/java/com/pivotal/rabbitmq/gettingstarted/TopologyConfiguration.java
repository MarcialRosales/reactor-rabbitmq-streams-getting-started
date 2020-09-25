package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class TopologyConfiguration {

    @Value("${exchange:numbers}")
    String NUMBERS;
    @Value("${queue:numbers}")
    String CONSUMER;

    @Bean
    public Consumer<TopologyBuilder> topology() {
        // @formatter:off
        return (builder) -> builder
                .declareExchange(NUMBERS)
                .and()
                .declareQueue(CONSUMER).boundTo(NUMBERS).withMaxLength(5);
        // @formatter:on
    }

}
