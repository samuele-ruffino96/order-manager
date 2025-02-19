package com.company.app.ordermanager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {

    /**
     * Configures the tasks for scheduling by providing a {@link ThreadPoolTaskScheduler}
     * to the {@link ScheduledTaskRegistrar}. The scheduler is configured with a fixed
     * thread pool size, a thread name prefix, and is initialized before being set.
     *
     * @param registrar the {@link ScheduledTaskRegistrar} used to register and configure
     *                  scheduled tasks, accepting the custom task scheduler
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);  // Number of threads for scheduled tasks
        scheduler.setThreadNamePrefix("Scheduled-");
        scheduler.initialize();
        registrar.setTaskScheduler(scheduler);
    }
}
