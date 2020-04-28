package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.topology.TopologyBuilder;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class SupplierApplication {
    private static Logger log = LoggerFactory.getLogger(SupplierApplication.class);
    @Value("${delay:1s}")
    Duration delay;

    public SupplierApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(SupplierApplication.class, args);
    }

    @Bean
    public Consumer<TopologyBuilder> topology(@Value("${output:numbers}") String exchange) {
        return (builder) -> {
            builder.declareExchange(exchange);
        };
    }

    @Bean("numbers")
    public Supplier<Flux<Integer>> eventSupplier() {
        return () -> {
            return Flux.range(1, Integer.MAX_VALUE).delayElements(this.delay).doOnNext((e) -> {
                log.info("Supplying {}", e);
            });
        };
    }
}
