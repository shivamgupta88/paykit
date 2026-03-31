package com.paykit.concurrency;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {

    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        return buildExecutor(5, 20, 100, "paykit-email-");
    }

    @Bean(name = "pdfExecutor")
    public Executor pdfExecutor() {
        return buildExecutor(3, 10, 50, "paykit-pdf-");
    }

    @Bean(name = "webhookExecutor")
    public Executor webhookExecutor() {
        return buildExecutor(10, 50, 500, "paykit-webhook-");
    }

    private ThreadPoolTaskExecutor buildExecutor(int core, int max, int queue, String prefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setThreadNamePrefix(prefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
