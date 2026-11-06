package ru.itmo.dentistry.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class CircuitBreakerConfigTest {

    @Test
    void catalogBreakerIgnoresNotFoundAndOpensAfterRepeatedFailures() throws Exception {
        Map<String, Object> catalog = catalogBreaker();

        assertEquals("count_based", catalog.get("sliding-window-type"));
        assertEquals(10, catalog.get("sliding-window-size"));
        assertEquals(5, catalog.get("minimum-number-of-calls"));
        assertEquals(50, catalog.get("failure-rate-threshold"));
        assertEquals("10s", catalog.get("wait-duration-in-open-state"));
        assertEquals(2, catalog.get("permitted-number-of-calls-in-half-open-state"));
        assertEquals(List.of("feign.FeignException$NotFound"), catalog.get("ignore-exceptions"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> catalogBreaker() throws Exception {
        Map<String, Object> root = new Yaml().load(Files.readString(config()));
        Map<String, Object> resilience4j = (Map<String, Object>) root.get("resilience4j");
        Map<String, Object> circuitbreaker = (Map<String, Object>) resilience4j.get("circuitbreaker");
        Map<String, Object> instances = (Map<String, Object>) circuitbreaker.get("instances");
        return (Map<String, Object>) instances.get("catalog");
    }

    private static Path config() {
        Path fromModule = Path.of("..", "config-repo", "appointment-service", "application.yml").normalize();
        if (Files.exists(fromModule)) {
            return fromModule;
        }
        return Path.of("config-repo", "appointment-service", "application.yml");
    }
}
