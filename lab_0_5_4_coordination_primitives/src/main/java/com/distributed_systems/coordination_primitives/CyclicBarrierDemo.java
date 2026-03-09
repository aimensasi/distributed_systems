package com.distributed_systems.coordination_primitives;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class CyclicBarrierDemo {

    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
        CyclicBarrier barrier = new CyclicBarrier(5);

        for (int i = 1; i <= 5; i++) {
            new Thread(() -> {
                Random random = new Random();
                int duration = random.nextInt(500, 1000);
                try {
                    Thread.sleep(duration);
                    System.out.printf("Thread %s Reached barrier\n", Thread.currentThread().getName());
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                }
                
                System.out.printf("Thread %s Started Phase 2\n", Thread.currentThread().getName());
            }).start();
        }


        // System.out.printf("Waiting for all threads to complete\n");
        // latch.await();
        
        System.out.printf("all threads to completed\n");
    }
}
