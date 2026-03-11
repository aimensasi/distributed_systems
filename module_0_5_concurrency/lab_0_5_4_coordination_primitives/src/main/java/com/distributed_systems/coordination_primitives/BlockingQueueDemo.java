package com.distributed_systems.coordination_primitives;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockingQueueDemo {
    BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
    AtomicInteger itemId = new AtomicInteger();

    public void producer(List<Thread> threads) throws InterruptedException{

        for (int i = 1; i <= 2; i++) {
            threads.add(new Thread(() -> {
                for (int k = 1; k <= 20; k++) {
                    try {
                        int id = itemId.incrementAndGet();
                        queue.put(id);
                        System.out.println("Adding Item " + id + " by thread " + Thread.currentThread().getName());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                queue.add(-1);
            }));
        }
    }

    public void consumer(List<Thread> threads) throws InterruptedException{
        for (int i = 1; i <= 3; i++) {
            threads.add(new Thread(() -> {
                while (true) {
                    try {
                        int item = queue.take();
                        if(item == -1){
                            try { queue.put(-1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                            break;
                        }

                        System.out.println("Consumed Item " + item + " by thread " + Thread.currentThread().getName());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        BlockingQueueDemo demo = new BlockingQueueDemo();
        List<Thread> threads = new ArrayList<>();
        demo.producer(threads);
        demo.consumer(threads);

        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            thread.join();
        }
    }
}
