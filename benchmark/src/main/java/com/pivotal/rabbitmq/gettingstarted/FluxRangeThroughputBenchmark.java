package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.stream.Transaction;
import com.pivotal.rabbitmq.stream.TransactionalConsumerStream;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;

@Configuration
public class FluxRangeThroughputBenchmark {

    @Bean
    @ConditionalOnProperty(name = "type", havingValue = "flux-range-throughput", matchIfMissing = false)
    public CommandLineRunner fluxRangeThroughput() {
        return (args) -> {
            StringBuilder sb = new StringBuilder("Flux.range benchmark: ")
                    .append("\n\t- Benchmark Settings: ")
                    .append("\n\t\t- producer-delay: ").append(delay.isPresent() ? delay.get() : "none");

            log.info(sb.toString());

            Flux<Integer> noDelay = Flux.just(1);
            Flux<Integer> integers = Flux
                    .range(1, count)
                    .delayUntil(i->noDelay);
            if (delay.isPresent()) integers = integers.delayElements(delay.get());

            integers
                    .window(Duration.ofSeconds(1))
                    .flatMap(Flux::count)
                    .doOnNext(new CollectAndPrintStats("Flux.Range"))
                    .blockLast();

        };
    }


    @Autowired
    RabbitEndpointService rabbit;

    @Value("${count:10000}")
    int count;
    @Value("${delay:#{null}}")
    Optional<Duration> delay;

    private static Logger log = LoggerFactory.getLogger(FluxRangeThroughputBenchmark.class);

}
