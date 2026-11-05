package ru.itmo.dentistry.catalog.procedure;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/procedures")
@Tag(name = "Dental procedures", description = "Procedure catalogue")
public class ProcedureController {

    private final ProcedureService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a catalogue procedure")
    Mono<DentalProcedureResponse> create(@Valid @RequestBody DentalProcedureRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a procedure by id")
    Mono<DentalProcedureResponse> findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping
    @Operation(summary = "Get procedures with total count in X-Total-Count")
    Mono<List<DentalProcedureResponse>> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            ServerHttpResponse response) {
        return service.findAll(page, size).map(pageResult -> {
            response.getHeaders().set("X-Total-Count", Long.toString(pageResult.total()));
            return pageResult.items();
        });
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a catalogue procedure")
    Mono<DentalProcedureResponse> update(@PathVariable UUID id, @Valid @RequestBody DentalProcedureRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a catalogue procedure")
    Mono<Void> deactivate(@PathVariable UUID id) {
        return service.deactivate(id);
    }
}
