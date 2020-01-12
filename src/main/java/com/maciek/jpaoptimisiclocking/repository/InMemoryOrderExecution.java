package com.maciek.jpaoptimisiclocking.repository;

import org.springframework.stereotype.Repository;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Repository
public class InMemoryOrderExecution {

    private Queue<String> executions;

    InMemoryOrderExecution() {
        executions = new ConcurrentLinkedQueue<>();
    }

    public void add(String s) {
        executions.add(s);
    }

    public Queue<String> getExecutions() {
        return executions;
    }

    public void reset() {
        executions = new ConcurrentLinkedQueue<>();
    }
}
