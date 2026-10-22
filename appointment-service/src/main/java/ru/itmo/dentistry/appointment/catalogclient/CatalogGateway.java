package ru.itmo.dentistry.appointment.catalogclient;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.UUID;
import org.springframework.stereotype.Service;
import ru.itmo.dentistry.appointment.common.CatalogUnavailableException;
import ru.itmo.dentistry.appointment.common.NotFoundException;

@Service
public class CatalogGateway {

    private final CatalogClient client;

    public CatalogGateway(CatalogClient client) {
        this.client = client;
    }

    @CircuitBreaker(name = "catalog", fallbackMethod = "userUnavailable")
    public CatalogUserView user(UUID id) {
        try {
            return client.user(id);
        } catch (FeignException.NotFound exception) {
            throw new NotFoundException("User not found: " + id);
        }
    }

    @CircuitBreaker(name = "catalog", fallbackMethod = "procedureUnavailable")
    public CatalogProcedureView procedure(UUID id) {
        try {
            return client.procedure(id);
        } catch (FeignException.NotFound exception) {
            throw new NotFoundException("Dental procedure not found: " + id);
        }
    }

    @SuppressWarnings("unused")
    private CatalogUserView userUnavailable(UUID id, Throwable cause) {
        if (cause instanceof NotFoundException notFound) {
            throw notFound;
        }
        throw new CatalogUnavailableException("Catalog service is temporarily unavailable");
    }

    @SuppressWarnings("unused")
    private CatalogProcedureView procedureUnavailable(UUID id, Throwable cause) {
        if (cause instanceof NotFoundException notFound) {
            throw notFound;
        }
        throw new CatalogUnavailableException("Catalog service is temporarily unavailable");
    }
}
