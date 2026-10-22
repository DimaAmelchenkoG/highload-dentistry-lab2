package ru.itmo.dentistry.catalog.procedure;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequestMapping("/api/v1/procedures")
@Tag(name = "Dental procedures", description = "Procedure catalogue")
public class ProcedureController {

    private final ProcedureService service;

    public ProcedureController(ProcedureService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a catalogue procedure")
    Mono<ResponseEntity<DentalProcedureResponse>> create(@Valid @RequestBody DentalProcedureRequest request) {
        return service.create(request)
                .map(created -> ResponseEntity
                        .created(URI.create("/api/v1/procedures/" + created.id()))
                        .body(created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a procedure by id")
    Mono<ResponseEntity<DentalProcedureResponse>> findById(@PathVariable UUID id) {
        return service.findById(id).map(ResponseEntity::ok);
    }

    @GetMapping
    @Operation(summary = "Get procedures with total count in X-Total-Count")
    Mono<ResponseEntity<java.util.List<DentalProcedureResponse>>> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.findAll(page, size)
                .map(pageResult -> ResponseEntity.ok()
                        .header("X-Total-Count", Long.toString(pageResult.total()))
                        .body(pageResult.items()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a catalogue procedure")
    Mono<ResponseEntity<DentalProcedureResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody DentalProcedureRequest request) {
        return service.update(id, request).map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a catalogue procedure")
    Mono<ResponseEntity<Void>> deactivate(@PathVariable UUID id) {
        return service.deactivate(id).thenReturn(ResponseEntity.noContent().build());
    }
}
