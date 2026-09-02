package com.aifincontroller;

import com.aifincontroller.config.AiProperties;
import com.aifincontroller.config.RazorpayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        AiProperties.class,
        RazorpayProperties.class
})
public class AiFinControllerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiFinControllerApplication.class, args);
    }
}
