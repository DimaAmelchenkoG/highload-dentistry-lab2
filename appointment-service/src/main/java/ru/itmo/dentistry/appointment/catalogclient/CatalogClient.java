package ru.itmo.dentistry.appointment.catalogclient;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "catalog-service")
public interface CatalogClient {

    @GetMapping("/internal/users/{id}")
    CatalogUserView user(@PathVariable("id") UUID id);

    @GetMapping("/internal/procedures/{id}")
    CatalogProcedureView procedure(@PathVariable("id") UUID id);
}
