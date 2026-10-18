package com.tracemind.order;

import com.tracemind.order.telemetry.TracingRestTemplateInterceptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(TracingRestTemplateInterceptor interceptor) {
        RestTemplate template = new RestTemplate();
        template.setInterceptors(Collections.singletonList(interceptor));
        return template;
    }
}
