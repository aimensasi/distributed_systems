package com.distributed_systems.thread_safety;

import java.util.ArrayList;
import java.util.List;

public class ThreadSafetyApplication {

    public static void main(String[] args) {
        UnsafeCounter unsafeCounter = new UnsafeCounter();
        List<Thread> unsafeThreads = new ArrayList<>();

        for(int i = 0; i < 100; i++){
            unsafeThreads.add(new Thread(() -> {
                for(int j = 0; j < 1000; j++){
                    unsafeCounter.increment();
                }
            }));
        }
        
        SynchronizedCounter synchronizedCounter = new SynchronizedCounter();
        List<Thread> synchronizedThreads = new ArrayList<>();

        for(int i = 0; i < 100; i++){
            synchronizedThreads.add(new Thread(() -> {
                for(int j = 0; j < 1000; j++){
                    synchronizedCounter.increment();
                }
            }));
        }
        
        AtomicCounter atomicCounter = new AtomicCounter();
        List<Thread> atomicThreads = new ArrayList<>();

        for(int i = 0; i < 100; i++){
            atomicThreads.add(new Thread(() -> {
                for(int j = 0; j < 1000; j++){
                    atomicCounter.increment();
                }
            }));
        }

        

        
        unsafeThreads.forEach(Thread::start);
        synchronizedThreads.forEach(Thread::start);
        atomicThreads.forEach(Thread::start);


        System.out.println("Expected counter value: 100000, Actual counter value: " + unsafeCounter.counter());
        System.out.println("Expected counter value: 100000, Actual counter value: " + synchronizedCounter.counter());
        System.out.println("Expected counter value: 100000, Actual counter value: " + atomicCounter.counter());
    }
}
