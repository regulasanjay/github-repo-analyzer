package com.githubassistant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AppConfig implements WebMvcConfigurer {
    private final ApiRateLimitInterceptor rateLimitInterceptor;
    public AppConfig(ApiRateLimitInterceptor rateLimitInterceptor){this.rateLimitInterceptor=rateLimitInterceptor;}
    @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(rateLimitInterceptor);}
    @Bean RestClient restClient(){return RestClient.builder().build();}
    @Bean Executor taskExecutor(){var e=new ThreadPoolTaskExecutor();e.setCorePoolSize(2);e.setMaxPoolSize(4);e.setQueueCapacity(50);e.setThreadNamePrefix("repo-index-");e.initialize();return e;}
}
