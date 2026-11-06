package ru.itmo.dentistry.configserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;

@SpringBootTest(properties = {
        "server.port=0",
        "spring.cloud.config.server.native.search-locations=classpath:/config-fixture/{application}/"
})
class ConfigServerApplicationTest {

    @Autowired
    private EnvironmentRepository repository;

    @Test
    void servesNativeConfigByApplicationName() {
        Environment environment = repository.findOne("catalog-service", "default", "main");

        assertEquals("catalog-service", environment.getName());
        assertTrue(environment.getPropertySources().stream()
                .anyMatch(source -> "catalog-native".equals(String.valueOf(source.getSource().get("demo.marker")))));
    }
}
