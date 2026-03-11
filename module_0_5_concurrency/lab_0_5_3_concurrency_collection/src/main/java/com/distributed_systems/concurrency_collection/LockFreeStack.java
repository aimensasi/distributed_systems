package com.distributed_systems.concurrency_collection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class LockFreeStack<T> {
    private final AtomicReference<Node<T>> top = new AtomicReference<>(null);

    private static class Node<T> {
        final T value;
        final Node<T> next;
        Node(T value, Node<T> next) {
            this.value = value;
            this.next = next;
        }
    }

    public void push(T value) {
        // your implementation
        while (true) { 
            Node<T> current = top.get();
            Node<T> node = new Node<>(value, current);

            if(top.compareAndSet(current, node)){
                return;
            }
        }
    }

    public T pop() {
        // returns null if empty
        // your implementation
        while (true) { 
            Node<T> node = top.get();

            if(node == null){
                return null;
            }


            if(top.compareAndSet(node, node.next)){
                return node.value;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int passed = 0, failed = 0;

        // ── Single-threaded correctness ──────────────────────────────────────

        LockFreeStack<Integer> stack = new LockFreeStack<>();

        // Empty stack returns null
        if (check(stack.pop() == null, "Test 1 - pop on empty stack returns null")) passed++; else failed++;

        // Push then pop returns same value
        stack.push(1);
        if (check(stack.pop() == 1, "Test 2 - push(1) then pop returns 1")) passed++; else failed++;

        // Stack is empty again after pop
        if (check(stack.pop() == null, "Test 3 - stack empty after pop")) passed++; else failed++;

        // LIFO order — last in, first out
        stack.push(10);
        stack.push(20);
        stack.push(30);
        if (check(stack.pop() == 30, "Test 4 - LIFO: pop returns 30 (last pushed)")) passed++; else failed++;
        if (check(stack.pop() == 20, "Test 5 - LIFO: pop returns 20")) passed++; else failed++;
        if (check(stack.pop() == 10, "Test 6 - LIFO: pop returns 10 (first pushed)")) passed++; else failed++;
        if (check(stack.pop() == null, "Test 7 - stack empty after all pops")) passed++; else failed++;

        // ── Concurrent correctness ───────────────────────────────────────────

        // 8 threads each push 1000 items — total items on stack must equal 8000
        LockFreeStack<Integer> concStack = new LockFreeStack<>();
        int threadCount = 8;
        int pushesPerThread = 1000;
        List<Thread> pushers = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            pushers.add(new Thread(() -> {
                for (int j = 0; j < pushesPerThread; j++) {
                    concStack.push(threadId * pushesPerThread + j);
                }
            }));
        }

        pushers.forEach(Thread::start);
        for (Thread t : pushers) t.join();

        // Count items on stack by popping everything
        int totalItems = 0;
        while (concStack.pop() != null) totalItems++;
        int expected = threadCount * pushesPerThread;
        if (check(totalItems == expected,
                "Test 8 - concurrent push: expected " + expected + " items, got " + totalItems)) passed++; else failed++;

        // 8 threads push 1000 items each, then 8 threads pop everything
        // No item should be lost, no item should be returned twice
        LockFreeStack<Integer> mixedStack = new LockFreeStack<>();
        List<Thread> mixedPushers = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            mixedPushers.add(new Thread(() -> {
                for (int j = 0; j < pushesPerThread; j++) {
                    mixedStack.push(threadId * pushesPerThread + j);
                }
            }));
        }
        mixedPushers.forEach(Thread::start);
        for (Thread t : mixedPushers) t.join();

        AtomicInteger popCount = new AtomicInteger(0);
        List<Thread> poppers = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            poppers.add(new Thread(() -> {
                Integer val;
                while ((val = mixedStack.pop()) != null) {
                    popCount.incrementAndGet();
                }
            }));
        }
        poppers.forEach(Thread::start);
        for (Thread t : poppers) t.join();

        if (check(popCount.get() == expected,
                "Test 9 - concurrent pop: popped " + popCount.get() + " of " + expected + " items")) passed++; else failed++;

        // Stack must be empty after all concurrent pops
        if (check(mixedStack.pop() == null,
                "Test 10 - stack empty after concurrent pops")) passed++; else failed++;

        System.out.println("\n" + passed + " passed, " + failed + " failed");
    }

    private static boolean check(boolean condition, String label) {
        System.out.println((condition ? "PASS" : "FAIL") + " — " + label);
        return condition;
    }
}