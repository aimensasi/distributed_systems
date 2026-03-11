package com.distributed_systems.thread_executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolDemo {

    private Runnable task(String label){
        Runnable task = () -> {
            System.out.println("Task " + label + " running on: " + Thread.currentThread().getName());
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        };

        return task;
    }

    public void fixedUnbounded() throws InterruptedException{
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
            5, 10, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>()
        );

        for (int i = 0; i < 10; i++) {
            try {
                pool.execute(task("task-" + (i + 1)));
            } catch (RejectedExecutionException e) {
                System.out.println("Task task-" + (i + 1) + " was REJECTED");
            }
        }

        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
        pool.close();
    }

    public void fixedbounded() throws InterruptedException{
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
            5, 10, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(3)
        );

        for (int i = 0; i < 10; i++) {
            try {
                pool.execute(task("task-" + (i + 1)));
            } catch (RejectedExecutionException e) {
                System.out.println("Task task-" + (i + 1) + " was REJECTED");
            }
        }

        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
        pool.close();
    }

    public void cachedThread() throws InterruptedException{
        ExecutorService pool = Executors.newCachedThreadPool();

        for (int i = 0; i < 10; i++) {
            try {
                pool.execute(task("task-" + (i + 1)));
            } catch (RejectedExecutionException e) {
                System.out.println("Task task-" + (i + 1) + " was REJECTED");
            }
        }

        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
    }

    public void custom() throws InterruptedException{
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
            5, 6, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(3),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        for (int i = 0; i < 10; i++) {
            try {
                pool.execute(task("task-" + (i + 1)));
            } catch (RejectedExecutionException e) {
                System.out.println("Task task-" + (i + 1) + " was REJECTED");
            }
        }

        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
        pool.close();
    }

    public static void main(String[] args) throws InterruptedException {
        ThreadPoolDemo demo = new ThreadPoolDemo();
        System.err.println("Unbounded Executor");
        demo.fixedUnbounded();
        System.err.println("Bounded Executor");
        demo.fixedbounded();
        System.err.println("Cached Executor");
        demo.cachedThread();
        System.err.println("Custom Executor");
        demo.custom();
    }
}
