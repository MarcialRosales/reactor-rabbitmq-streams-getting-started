package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.topology.TopologyBuilder;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ProcessorApplication {
    private static Logger log = LoggerFactory.getLogger(ProcessorApplication.class);

    public ProcessorApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(ProcessorApplication.class, args);
    }

    @Bean
    public Consumer<TopologyBuilder> topology(@Value("${output:doublenumbers}") String output,
                                              @Value("${input:numbers}") String input) {
        return (builder) -> {
            builder.declareExchange(input)
                    .and()
                    .declareQueue(input)
                    .boundTo(input)
                        .and()
                    .declareExchange(output);
        };
    }

    @Bean("multiplier")
    public Function<Integer, Integer> multiplier() {
        return (e) -> {
            return e * 2;
        };
    }
}

