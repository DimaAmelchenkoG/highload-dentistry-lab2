package ru.itmo.dentistry.catalog.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Patients, doctors and clinic staff")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a user")
    Mono<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return userService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by id")
    Mono<UserResponse> findById(@PathVariable UUID id) {
        return userService.findById(id);
    }

    @GetMapping
    @Operation(summary = "Get a paginated user list")
    Mono<Page<UserResponse>> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(required = false) UserRole role) {
        return userService.findAll(page, size, role);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user")
    Mono<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a user")
    Mono<Void> deactivate(@PathVariable UUID id) {
        return userService.deactivate(id);
    }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a user from the database")
    Mono<Void> deletePermanently(@PathVariable UUID id) {
        return userService.deletePermanently(id);
    }

    @PutMapping("/{doctorId}/procedures/{procedureId}")
    @Operation(summary = "Assign a dental procedure to a doctor")
    Mono<UserResponse> assignProcedure(@PathVariable UUID doctorId, @PathVariable UUID procedureId) {
        return userService.assignProcedure(doctorId, procedureId);
    }

    @DeleteMapping("/{doctorId}/procedures/{procedureId}")
    @Operation(summary = "Remove a dental procedure from a doctor")
    Mono<UserResponse> removeProcedure(@PathVariable UUID doctorId, @PathVariable UUID procedureId) {
        return userService.removeProcedure(doctorId, procedureId);
    }
}
