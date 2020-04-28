package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.ReactiveRabbit;
import com.pivotal.rabbitmq.stream.Transaction;
import com.pivotal.rabbitmq.stream.TransactionManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;

public class DefaultStreamSource<T> implements StreamSource<T> {
    private UnicastProcessor<Transaction<T>> stream;
    private FluxSink<Transaction<T>> sink;
    private ReactiveRabbit rabbit;
    private TransactionManager<T, Transaction<T>> transactionManager;

    public DefaultStreamSource(String name) {
        stream = UnicastProcessor.create();
        sink = stream.sink(FluxSink.OverflowStrategy.ERROR);
        transactionManager = new TransactionManager<>(name);
    }

    Flux<Transaction<T>> stream() {
        return stream.subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public void sendAndForget(T shipment) {
        sink.next(transactionManager.newTransaction(shipment));
    }

    @Override
    public Mono<T> send(T shipment) {
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            sink.next(transactionManager.newTransaction(shipment, future));
        }catch(Throwable t) {
            future.completeExceptionally(t);
        }
        return Mono.fromCompletionStage(future);
    }
}
