package com.distributed_systems.thread_safety;

public class VisibilityDemo {
    private volatile static boolean running = true;  // NO volatile

    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            int count = 0;
            while (running) {   // reads running from CPU cache
                count++;
            }
            System.out.println("Stopped after " + count + " iterations");
        });

        worker.start();
        Thread.sleep(1000);
        running = false;  // writes to main memory but worker's cache may not update
        System.out.println("Set running to false");
    }
}