package com.distributed_systems.thread_safety;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CheckThenActDemo {
    private AtomicBoolean initialized = new AtomicBoolean(false);
    private List<String> sharedList = new ArrayList<>();

    // UNSAFE version
    public void initializeIfNeeded() {
        if (initialized.compareAndSet(false, true)) {
            // simulate some work
            sharedList.add("initialized by " + 
                Thread.currentThread().getName());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        CheckThenActDemo demo = new CheckThenActDemo();
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            threads.add(new Thread(demo::initializeIfNeeded));
        }

        threads.forEach(Thread::start);
        for (Thread t : threads) t.join();

        System.out.println("Initialized by " + 
            demo.sharedList.size() + " threads");
        System.out.println("Expected: 1");
    }
}
