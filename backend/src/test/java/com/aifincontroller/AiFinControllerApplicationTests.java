package com.aifincontroller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies the Spring application context starts successfully.
 * Requires a running PostgreSQL instance at localhost:5432/aifincontroller.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=none"
})
class AiFinControllerApplicationTests {

    @Test
    void contextLoads() {
        // Context must start without errors
    }
}
