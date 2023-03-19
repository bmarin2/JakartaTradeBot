package com.tradebot.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import lombok.Data;

@Data
@ApplicationScoped
public class TaskService {
    
    public static final Logger LOGGER = Logger.getLogger(TaskService.class.getName());

    private ScheduledExecutorService executorService;

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        executorService = Executors.newScheduledThreadPool(8); // creates a thread pool with 5 threads
    }

    public void addTask(String taskId, Runnable task, long initialDelay, long delay, TimeUnit unit) {
        ScheduledFuture<?> scheduledFuture = executorService.scheduleWithFixedDelay(task, initialDelay, delay, unit);
        scheduledTasks.put(taskId, scheduledFuture);
    }

    public void removeTask(String taskId) {
        ScheduledFuture<?> scheduledFuture = scheduledTasks.get(taskId);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledTasks.remove(taskId);
        }
    }
}