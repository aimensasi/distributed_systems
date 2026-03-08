package com.distributed_systems.concurrency_collection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentCollectionsDemo {
    Map<String, Integer> concurrentMap = new ConcurrentHashMap<>();
    final Map<String, Integer> syncMap = new HashMap<>();


    public void workonSyncMap(List<Thread> threads){

        for (int i = 1; i <= 8; i++) {
            threads.add(new Thread(() -> {
                Random random = new Random();
                for (int k = 1; k <= 10_000; k++) {
                    synchronized (syncMap) {
                        String key = "key" + random.nextInt(100);
                        int value = random.nextInt(1000);

                        if(random.nextBoolean()){
                            syncMap.put(key, value);
                        } else {
                            syncMap.get(key);
                        }
                    }
                }
            }));
        }
    }

    public void  workonConcurrentMap(List<Thread> threads){


        for (int i = 1; i <= 8; i++) {
            threads.add(new Thread(() -> {
                Random random = new Random();
                for (int k = 1; k <= 10_000; k++) {
                    String key = "key" + random.nextInt(100);
                    int value = random.nextInt(1000);

                    if(random.nextBoolean()){
                        concurrentMap.put(key, value);
                    } else {
                        concurrentMap.get(key);
                    }
                }
            }));
        }
    }


    public static void main(String[] args) throws InterruptedException {
       ConcurrentCollectionsDemo demo = new ConcurrentCollectionsDemo();
       // Scenario 1 — run and wait
        List<Thread> syncThreads = new ArrayList<>();
        demo.workonSyncMap(syncThreads);
        long syncStart = System.nanoTime();
        syncThreads.forEach(Thread::start);
        for (Thread t : syncThreads) t.join();
        long syncTotal = (System.nanoTime() - syncStart) / 1_000_000;
        System.out.println("=== SyncMap total wall-clock: " + syncTotal + "ms ===");

        // Scenario 2 — run and wait
        List<Thread> concurrentThreads = new ArrayList<>();
        demo.workonConcurrentMap(concurrentThreads);
        long concurrentStart = System.nanoTime();
        concurrentThreads.forEach(Thread::start);
        for (Thread t : concurrentThreads) t.join();
        long concurrentTotal = (System.nanoTime() - concurrentStart) / 1_000_000;
        System.out.println("=== ConcurrentHashMap total wall-clock: " + concurrentTotal + "ms ===");
    }
}
