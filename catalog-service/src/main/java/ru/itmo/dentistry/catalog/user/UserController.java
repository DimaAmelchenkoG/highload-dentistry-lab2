package ru.itmo.dentistry.catalog.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
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
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Patients, doctors and clinic staff")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Create a user")
    Mono<ResponseEntity<UserResponse>> create(@Valid @RequestBody UserRequest request) {
        return userService.create(request)
                .map(created -> ResponseEntity.created(URI.create("/api/v1/users/" + created.id())).body(created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by id")
    Mono<ResponseEntity<UserResponse>> findById(@PathVariable UUID id) {
        return userService.findById(id).map(ResponseEntity::ok);
    }

    @GetMapping
    @Operation(summary = "Get a paginated user list")
    Mono<ResponseEntity<Page<UserResponse>>> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(required = false) UserRole role) {
        return userService.findAll(page, size, role).map(ResponseEntity::ok);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user")
    Mono<ResponseEntity<UserResponse>> update(@PathVariable UUID id, @Valid @RequestBody UserRequest request) {
        return userService.update(id, request).map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a user")
    Mono<ResponseEntity<Void>> deactivate(@PathVariable UUID id) {
        return userService.deactivate(id).thenReturn(ResponseEntity.noContent().build());
    }

    @PutMapping("/{doctorId}/procedures/{procedureId}")
    @Operation(summary = "Assign a dental procedure to a doctor")
    Mono<ResponseEntity<UserResponse>> assignProcedure(
            @PathVariable UUID doctorId, @PathVariable UUID procedureId) {
        return userService.assignProcedure(doctorId, procedureId).map(ResponseEntity::ok);
    }

    @DeleteMapping("/{doctorId}/procedures/{procedureId}")
    @Operation(summary = "Remove a dental procedure from a doctor")
    Mono<ResponseEntity<UserResponse>> removeProcedure(
            @PathVariable UUID doctorId, @PathVariable UUID procedureId) {
        return userService.removeProcedure(doctorId, procedureId).map(ResponseEntity::ok);
    }
}
