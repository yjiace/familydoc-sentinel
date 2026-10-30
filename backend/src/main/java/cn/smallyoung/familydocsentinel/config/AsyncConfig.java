package cn.smallyoung.familydocsentinel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务与并行论断核验线程池配置
 * <p>
 * 分别提供：
 * 1. {@code factCheckTaskExecutor}：负责核验任务主流程责任链的异步调度；
 * 2. {@code claimVerifyExecutor}：负责单任务内多条论断的并行检索、立场标注与裁决执行。
 * </p>
 */
@EnableAsync
@Configuration
public class AsyncConfig {

    /**
     * 核验任务主流程异步执行器
     */
    @Bean({"factCheckTaskExecutor", "taskExecutor"})
    public Executor factCheckTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("factcheck-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 单论断并行核验专用执行器
     */
    @Bean("claimVerifyExecutor")
    public ThreadPoolTaskExecutor claimVerifyExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(12);
        executor.setMaxPoolSize(48);
        executor.setQueueCapacity(300);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("claim-verify-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

