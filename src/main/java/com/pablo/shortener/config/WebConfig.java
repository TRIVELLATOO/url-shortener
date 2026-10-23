package com.pablo.shortener.config;

import com.pablo.shortener.ratelimit.CreateLinkRateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CreateLinkRateLimitInterceptor createLinkRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(createLinkRateLimitInterceptor).addPathPatterns("/api/links");
    }
}
