package ru.itmo.dentistry.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class GatewayRoutesTest {

    @Test
    void routesUsersToCatalogAndAvailabilityToAppointments() throws Exception {
        List<Map<String, Object>> routes = routes();

        Map<String, Object> availability = route(routes, "doctor-availability");
        Map<String, Object> users = route(routes, "catalog-users");
        Map<String, Object> procedures = route(routes, "catalog-procedures");
        Map<String, Object> appointments = route(routes, "appointments");

        assertEquals("lb://appointment-service", availability.get("uri"));
        assertEquals(List.of("Path=/api/v1/users/*/availability"), availability.get("predicates"));
        assertEquals("lb://catalog-service", users.get("uri"));
        assertEquals(List.of("Path=/api/v1/users/**"), users.get("predicates"));
        assertEquals("lb://catalog-service", procedures.get("uri"));
        assertEquals("lb://appointment-service", appointments.get("uri"));
        assertTrue(indexOf(routes, "doctor-availability") < indexOf(routes, "catalog-users"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> routes() throws Exception {
        Map<String, Object> root = new Yaml().load(Files.readString(gatewayConfig()));
        Map<String, Object> spring = (Map<String, Object>) root.get("spring");
        Map<String, Object> cloud = (Map<String, Object>) spring.get("cloud");
        Map<String, Object> gateway = (Map<String, Object>) cloud.get("gateway");
        Map<String, Object> server = (Map<String, Object>) gateway.get("server");
        Map<String, Object> webflux = (Map<String, Object>) server.get("webflux");
        return (List<Map<String, Object>>) webflux.get("routes");
    }

    private static Path gatewayConfig() {
        Path fromModule = Path.of("..", "config-repo", "gateway", "application.yml").normalize();
        if (Files.exists(fromModule)) {
            return fromModule;
        }
        return Path.of("config-repo", "gateway", "application.yml");
    }

    private static Map<String, Object> route(List<Map<String, Object>> routes, String id) {
        return routes.stream()
                .filter(route -> id.equals(route.get("id")))
                .findFirst()
                .orElseThrow();
    }

    private static int indexOf(List<Map<String, Object>> routes, String id) {
        for (int i = 0; i < routes.size(); i++) {
            if (id.equals(routes.get(i).get("id"))) {
                return i;
            }
        }
        throw new IllegalStateException(id);
    }
}
