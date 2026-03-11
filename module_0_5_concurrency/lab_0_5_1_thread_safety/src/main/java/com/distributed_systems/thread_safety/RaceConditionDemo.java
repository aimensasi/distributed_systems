package com.distributed_systems.thread_safety;

import java.util.ArrayList;
import java.util.List;

public class RaceConditionDemo {

    public static void main(String[] args) throws InterruptedException {
        UnsafeCounter counter = new UnsafeCounter();
        List<Thread> threads = new ArrayList<>();

        for(int i = 0; i < 100; i++){
            threads.add(new Thread(() -> {
                for(int j = 0; j < 1000; j++){
                    counter.increment();
                }
            }));
        }

        threads.forEach(Thread::start);

        System.out.println("Expected counter value: 100000, Actual counter value: " + counter.counter());
    }
}
