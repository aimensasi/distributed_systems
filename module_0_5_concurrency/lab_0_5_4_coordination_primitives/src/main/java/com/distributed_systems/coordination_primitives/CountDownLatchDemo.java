package com.distributed_systems.coordination_primitives;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class CountDownLatchDemo {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);

        for (int i = 1; i <= 5; i++) {
            new Thread(() -> {
                Random random = new Random();
                int duration = random.nextInt(500, 1000);
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                }
                
                latch.countDown();
                System.out.printf("Passed Check Point For Thread %s\n", Thread.currentThread().getName());
            }).start();
        }


        System.out.printf("Waiting for all threads to complete\n");
        latch.await();
        System.out.printf("all threads to completed\n");
    }
}
