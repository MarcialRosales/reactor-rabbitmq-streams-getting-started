package com.pivotal.rabbitmq.gettingstarted;

import reactor.core.publisher.Mono;

public interface StreamSource<T> {
    void sendAndForget(T event);
    Mono<T> send(T event);
}
