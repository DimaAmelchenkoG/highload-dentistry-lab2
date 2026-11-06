package ru.itmo.dentistry.appointment.catalogclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import ru.itmo.dentistry.appointment.common.CatalogUnavailableException;
import ru.itmo.dentistry.appointment.common.NotFoundException;

class CatalogGatewayTest {

    private final CatalogGateway gateway = new CatalogGateway(Mockito.mock(CatalogClient.class));

    @Test
    void notFoundBecomesDomainNotFound() {
        UUID id = UUID.randomUUID();

        NotFoundException error = assertThrows(NotFoundException.class, () -> ReflectionTestUtils.invokeMethod(
                gateway, "userUnavailable", id, feignStatus(404)));

        assertTrue(error.getMessage().contains(id.toString()));
    }

    @Test
    void otherFailuresOpenAsCatalogUnavailable() {
        CatalogUnavailableException error = assertThrows(CatalogUnavailableException.class,
                () -> ReflectionTestUtils.invokeMethod(gateway, "procedureUnavailable", UUID.randomUUID(), feignStatus(503)));

        assertEquals("Catalog service is temporarily unavailable", error.getMessage());
    }

    private static FeignException feignStatus(int status) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://catalog-service/internal/users/1",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate());
        Response response = Response.builder()
                .status(status)
                .reason("error")
                .request(request)
                .headers(Map.of())
                .body(new byte[0])
                .build();
        return FeignException.errorStatus("CatalogClient#user(UUID)", response);
    }
}
