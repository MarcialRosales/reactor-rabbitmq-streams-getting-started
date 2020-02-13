package com.pivotal.rabbitmq.gettingstarted;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BasicSendAndReceiveApplicationTest {

    Logger log = LoggerFactory.getLogger(BasicSendAndReceiveApplicationTest.class);
    int count = 10;

    // Generate integers from 1 to 10
    // Then Multiple by 2
    // Introduce a delay of 1sec between integers ...
    // Before sending them to Rabbit

    @Test
    public void generateIntegersUsingForLoop() throws InterruptedException {
        for (int i = 1; i <= count; i++) {
            long multiplied = i * 2;
            log.info("Emitting {}", multiplied);
            delay(ofSeconds(1));
            sendToRabbit(multiplied);
        }
    }
    private void sendToRabbit(long number) {
        log.info("Sending {}", number);
    }



    @Test
    public void generateIntegersUsingStreamAPI() throws InterruptedException {
        CountDownLatch until = new CountDownLatch(count-1);

        IntStream numbers = IntStream.range(1, count)
                .peek(i -> log.info("Emitting {}", i));
        log.info("After building numbers");

        LongStream longNumbers = numbers
                //.parallel() //  Stream uses the ForkJoinPool.commonPool(), a Thread Pool shared by the entire application
                .mapToLong(i -> i * 2);

        log.info("After building longNumbers");
        longNumbers
                .forEach(l -> {
                    delay(ofSeconds(1));
                    sendToRabbit(l);
                    until.countDown();
                });

        assertEquals(0, until.getCount());
    }

    @Test
    public void generateIntegersUsingReactor() throws InterruptedException {
        Long receivedCount = Flux.range(1, count)
                .map(i -> i * 2L)
                .doOnNext(l -> log.info("Emitting {}", l))
            //    .limitRate(2)
                .delayElements(ofSeconds(1))
                .doOnNext(this::sendToRabbit)
                .count()
                .block();

        assertNotNull(receivedCount);
        assertEquals(count, receivedCount.longValue());
    }

    // What if we want to send these number to RabbitMQ


    private LongStream delayAndLog(long l) {
        delay(ofSeconds(1));
        log.info("Emitting {}", l);
        return LongStream.of(l);
    }
    @Test
    public void sendNumbersToRabbitUsingStreamAPI() {
        Map<Long, Long> map = new HashMap<>();

        IntStream.range(1, count)
                .mapToLong(i -> i * 2)
                .flatMap(this::delayAndLog)
                .forEach(this::sendToRabbit);
    }

    private void delay(Duration delay) {
        try {
            SECONDS.sleep(delay.getSeconds());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}