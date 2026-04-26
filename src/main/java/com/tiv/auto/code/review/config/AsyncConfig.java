package com.tiv.auto.code.review.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步配置类
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        // 核心线程数
        threadPoolTaskExecutor.setCorePoolSize(5);
        // 最大线程数
        threadPoolTaskExecutor.setMaxPoolSize(20);
        // 等待队列容量
        threadPoolTaskExecutor.setQueueCapacity(100);
        // 线程名称前缀
        threadPoolTaskExecutor.setThreadNamePrefix("auto-code-review:");
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

}