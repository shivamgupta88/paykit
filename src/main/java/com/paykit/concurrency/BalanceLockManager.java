package com.paykit.concurrency;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class BalanceLockManager {

    private final ConcurrentHashMap<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock getLock(UUID tenantId) {
        return locks.computeIfAbsent(tenantId, k -> new ReentrantLock(true));
    }

    public void executeWithLock(UUID tenantId, Runnable action) {
        ReentrantLock lock = getLock(tenantId);
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }
}
