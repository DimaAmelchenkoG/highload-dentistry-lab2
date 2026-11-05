package ru.itmo.dentistry.appointment.catalogclient;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.dentistry.appointment.common.CatalogUnavailableException;
import ru.itmo.dentistry.appointment.common.NotFoundException;

@Service
@RequiredArgsConstructor
public class CatalogGateway {

    private final CatalogClient client;

    @CircuitBreaker(name = "catalog", fallbackMethod = "userUnavailable")
    public CatalogUserView user(UUID id) {
        return client.user(id);
    }

    @CircuitBreaker(name = "catalog", fallbackMethod = "procedureUnavailable")
    public CatalogProcedureView procedure(UUID id) {
        return client.procedure(id);
    }

    @SuppressWarnings("unused")
    private CatalogUserView userUnavailable(UUID id, Throwable cause) {
        if (cause instanceof FeignException.NotFound) {
            throw new NotFoundException("User not found: " + id);
        }
        throw new CatalogUnavailableException("Catalog service is temporarily unavailable");
    }

    @SuppressWarnings("unused")
    private CatalogProcedureView procedureUnavailable(UUID id, Throwable cause) {
        if (cause instanceof FeignException.NotFound) {
            throw new NotFoundException("Dental procedure not found: " + id);
        }
        throw new CatalogUnavailableException("Catalog service is temporarily unavailable");
    }
}
