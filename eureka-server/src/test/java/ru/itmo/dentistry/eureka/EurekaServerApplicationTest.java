package ru.itmo.dentistry.eureka;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.server.wait-time-in-ms-when-sync-empty=0")
class EurekaServerApplicationTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    @Test
    void dashboardIsAvailable() throws Exception {
        HttpResponse<String> response = get("/");

        assertTrue(response.statusCode() >= 200 && response.statusCode() < 300);
        assertTrue(response.body().contains("Eureka"));
    }

    @Test
    void registryAcceptsApplicationQueries() throws Exception {
        HttpResponse<String> response = get("/eureka/apps");

        assertTrue(response.statusCode() >= 200 && response.statusCode() < 300);
        assertTrue(response.body().contains("applications"));
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
