package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.RabbitEndpointService;
import com.pivotal.rabbitmq.stream.Transaction;
import com.pivotal.rabbitmq.stream.TransactionalConsumerStream;
import com.pivotal.rabbitmq.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Date;
import java.util.Optional;

@Configuration
public class ThroughputBenchmark {

    private Disposable receiver() {
        TransactionalConsumerStream<Integer> receiverStream = rabbit
                .declareTopology(builder->builder
                        .declareExchange(NUMBERS)
                        .and()
                        .declareQueue(NUMBERS).boundTo(NUMBERS)
                )
                .createTransactionalConsumerStream(NUMBERS, Integer.class);
        prefetch.ifPresent(receiverStream::withPrefetch);
        if (ackEvery.isPresent() && ackAfter.isPresent()) {
            receiverStream.ackEvery(ackEvery.get(), ackAfter.get());
        }

        Disposable receiver = receiverStream
                .receive()
                .doOnNext(Transaction::commit)
                .window(Duration.ofSeconds(1))
                .flatMap(Flux::count)
                .doOnNext(new CollectAndPrintStats("Received"))
                .subscribe();

        return receiver;
    }
    private Disposable sender(Flux<Integer> events) {

        return rabbit
                .declareTopology(TopologyBuilder.NONE)
                .createProducerStream(Integer.class)
                .withAttributes()
                .timestamp(integer -> new Date())
                .and()
                .route()
                .toExchange(NUMBERS)
                .then()
                .send(events)
                .window(Duration.ofSeconds(1))
                .flatMap(Flux::count)
                .doOnNext(new CollectAndPrintStats("Sent"))
                .subscribe();

    }

    @Bean
    @ConditionalOnProperty(name = "type", havingValue = "throughput", matchIfMissing = false)
    public Disposable.Composite throughput() {

        StringBuilder sb = new StringBuilder("Throughput benchmark: ")
                .append("\n\t- Benchmark Settings: ")
                .append("\n\t\t- producer-delay: ").append(delay.isPresent() ? delay.get() : "none")

                .append("\n\t- Consumer Stream Settings: ")
                .append("\n\t\t- ackEvery: ").append(ackEvery.isPresent() ? ackEvery.get() : "default to 125")
                .append("\n\t\t- ackAfter: ").append(ackAfter.isPresent() ? ackAfter.get() : "default to 2sec")
                .append("\n\t\t- prefetch: ").append(prefetch.isPresent() ? prefetch.get() : "default to 250")
                .append("\n\n\t- Producer Stream Settings: ")
                .append("\n\t\t- maxInFlight: ").append(maxInFlight.isPresent() ? maxInFlight.get() : "default to 1")
                ;
        log.info(sb.toString());

        Flux<Integer> integers = Flux
                .range(1, count);
        if (delay.isPresent()) integers = integers.delayElements(delay.get());

        return Disposables.composite(receiver(), sender(integers));

    }


    @Autowired
    RabbitEndpointService rabbit;

    @Value("${exchange:numbers}")
    String NUMBERS;

    @Value("${count:10000}")
    int count;
    @Value("${delay:#{null}}")
    Optional<Duration> delay;

    @Value("${max-in-flight:#{null}}")
    Optional<Integer> maxInFlight;
    @Value("${ack-every:#{null}}")
    Optional<Integer> ackEvery;
    @Value("${ack-after:#{null}}")
    Optional<Duration> ackAfter;
    @Value("${prefetch:#{null}}")
    Optional<Integer> prefetch;

    @Value("${collect-and-print:true}") boolean collectAndPrint;

    private static Logger log = LoggerFactory.getLogger(ThroughputBenchmark.class);

}
