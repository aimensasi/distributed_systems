package com.distributed_systems.thread_safety;

public class UnsafeCounter {
    private int counter = 0;

    public void increment(){
        counter++;
    }
    
    public int counter(){
        return this.counter;
    }
}
