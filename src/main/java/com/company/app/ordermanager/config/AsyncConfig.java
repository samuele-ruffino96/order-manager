package com.company.app.ordermanager.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <p>Configuration class for enabling and customizing asynchronous task execution, in order to override default {@link SimpleAsyncTaskExecutor}</p>
 *
 * <p>This class implements the {@link AsyncConfigurer} interface,
 * allowing for the configuration of a custom {@link Executor} for asynchronous task execution
 * as well as handling uncaught exceptions in asynchronous methods.</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Set always alive thread pool size
        executor.setCorePoolSize(5);

        // Maximum number of threads.
        executor.setMaxPoolSize(10);

        // Size of the queue to hold tasks before blocking
        executor.setQueueCapacity(25);

        // Non-core threads keep alive timeout
        executor.setKeepAliveSeconds(60);

        // If both the queue and pool are full the calling thread executes the task
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setThreadNamePrefix("AsyncThread-");
        executor.initialize();

        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        // Custom exception handling for async tasks
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
