package com.distributed_systems.thread_safety;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicCounter {
    private AtomicInteger counter = new AtomicInteger(0);
    
    public void increment(){
        counter.incrementAndGet();
    }

    public int counter(){
        return counter.get();
    }
}
