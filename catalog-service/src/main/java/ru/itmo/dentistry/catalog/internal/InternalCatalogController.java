package ru.itmo.dentistry.catalog.internal;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.itmo.dentistry.catalog.procedure.ProcedureService;
import ru.itmo.dentistry.catalog.user.UserService;

@RestController
@RequestMapping("/internal")
public class InternalCatalogController {

    private final UserService userService;
    private final ProcedureService procedureService;

    public InternalCatalogController(UserService userService, ProcedureService procedureService) {
        this.userService = userService;
        this.procedureService = procedureService;
    }

    @GetMapping("/users/{id}")
    Mono<CatalogUserView> user(@PathVariable UUID id) {
        return userService.findInternalById(id);
    }

    @GetMapping("/procedures/{id}")
    Mono<CatalogProcedureView> procedure(@PathVariable UUID id) {
        return procedureService.findById(id)
                .map(procedure -> new CatalogProcedureView(
                        procedure.id(), procedure.name(), procedure.basePrice(), procedure.active()));
    }
}
