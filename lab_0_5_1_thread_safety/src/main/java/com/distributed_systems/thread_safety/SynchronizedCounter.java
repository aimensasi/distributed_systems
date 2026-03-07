package com.distributed_systems.thread_safety;

public class SynchronizedCounter {
    private int counter = 0;

    public synchronized void increment(){
        counter++;
    }

    public synchronized int counter(){
        return this.counter;
    }
}
