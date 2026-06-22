package com.example.kvm.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 将前端的所有页面路由（单页面应用路由）直接转发至 index.html 入口
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/host").setViewName("forward:/index.html");
        registry.addViewController("/vms").setViewName("forward:/index.html");
        registry.addViewController("/images").setViewName("forward:/index.html");
        registry.addViewController("/networks").setViewName("forward:/index.html");
        registry.addViewController("/snapshots").setViewName("forward:/index.html");
        registry.addViewController("/storage").setViewName("forward:/index.html");
        registry.addViewController("/vnc/**").setViewName("forward:/index.html");
    }
}
