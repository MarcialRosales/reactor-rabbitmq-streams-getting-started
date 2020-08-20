package com.pivotal.rabbitmq.gettingstarted;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class CollectAndPrintStats implements Consumer<Long> {
    private long total;
    private int acceptCount;
    String name;
    private static Logger log = LoggerFactory.getLogger(CollectAndPrintStats.class);

    public CollectAndPrintStats(String name) {
        this.name = name;
    }

    @Override
    public void accept(Long count) {
        total+=count;
        acceptCount++;
        log.debug("{} {}/sec events ({}). Total: {} ", name, count, acceptCount,  total);
    }
}
