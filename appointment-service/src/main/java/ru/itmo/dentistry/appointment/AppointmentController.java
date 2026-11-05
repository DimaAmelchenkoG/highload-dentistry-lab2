package ru.itmo.dentistry.appointment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments", description = "Appointment lifecycle and performed procedures")
public class AppointmentController {

    private final AppointmentService service;

    @PostMapping
    @Operation(summary = "Book an appointment")
    Mono<ResponseEntity<AppointmentResponse>> create(@Valid @RequestBody CreateAppointmentRequest request) {
        return blocking(() -> {
            var created = service.create(request);
            return ResponseEntity.created(URI.create("/api/v1/appointments/" + created.id())).body(created);
        });
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an appointment by id")
    Mono<ResponseEntity<AppointmentResponse>> findById(@PathVariable UUID id) {
        return blocking(() -> ResponseEntity.ok(service.findById(id)));
    }

    @GetMapping
    @Operation(summary = "Scroll appointments using a cursor without a total count")
    Mono<ResponseEntity<AppointmentScrollResponse>> findAll(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return blocking(() -> ResponseEntity.ok(service.findAll(cursor, size)));
    }

    @PatchMapping("/{id}/reschedule")
    @Operation(summary = "Reschedule an appointment atomically")
    Mono<ResponseEntity<AppointmentResponse>> reschedule(
            @PathVariable UUID id, @Valid @RequestBody RescheduleAppointmentRequest request) {
        return blocking(() -> ResponseEntity.ok(service.reschedule(id, request)));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Complete an appointment")
    Mono<ResponseEntity<AppointmentResponse>> complete(@PathVariable UUID id) {
        return blocking(() -> ResponseEntity.ok(service.complete(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel an appointment")
    Mono<ResponseEntity<Void>> cancel(@PathVariable UUID id) {
        return blocking(() -> {
            service.cancel(id);
            return ResponseEntity.noContent().build();
        });
    }

    @PostMapping("/{id}/procedures")
    @Operation(summary = "Add a performed procedure with a price snapshot")
    Mono<ResponseEntity<AppointmentResponse>> addProcedure(
            @PathVariable UUID id, @Valid @RequestBody AppointmentProcedureRequest request) {
        return blocking(() -> ResponseEntity.ok(service.addProcedure(id, request)));
    }

    @PutMapping("/{appointmentId}/procedures/{itemId}")
    @Operation(summary = "Update a performed procedure")
    Mono<ResponseEntity<AppointmentResponse>> updateProcedure(
            @PathVariable UUID appointmentId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateAppointmentProcedureRequest request) {
        return blocking(() -> ResponseEntity.ok(service.updateProcedure(appointmentId, itemId, request)));
    }

    @DeleteMapping("/{appointmentId}/procedures/{itemId}")
    @Operation(summary = "Delete a performed procedure")
    Mono<ResponseEntity<Void>> removeProcedure(@PathVariable UUID appointmentId, @PathVariable UUID itemId) {
        return blocking(() -> {
            service.removeProcedure(appointmentId, itemId);
            return ResponseEntity.noContent().build();
        });
    }

    private <T> Mono<T> blocking(java.util.concurrent.Callable<T> action) {
        return Mono.fromCallable(action).subscribeOn(Schedulers.boundedElastic());
    }
}
