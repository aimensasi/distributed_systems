package com.distributed_systems.coordination_primitives;

import java.util.concurrent.BrokenBarrierException;

public class DeadLockDemo {
    Object objA = new Object();
    Object objB = new Object();

    public static void sleep(){
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void run() throws InterruptedException{
        Thread thread1 = new Thread(() -> {
            synchronized (objA) {
                sleep();
                synchronized (objB) {
                    
                }
            }
        });

        Thread thread2 = new Thread(() -> {
            synchronized (objA) {
                sleep();
                synchronized (objB) {
                    
                }
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
    }
    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
        DeadLockDemo demo = new DeadLockDemo();
        demo.run();
    }
}
