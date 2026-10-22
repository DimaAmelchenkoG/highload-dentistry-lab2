package ru.itmo.dentistry.catalog.procedure;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import ru.itmo.dentistry.catalog.common.ConflictException;
import ru.itmo.dentistry.catalog.common.NotFoundException;

@Service
public class ProcedureService {

    private final ProcedureRepository repository;
    private final TransactionalOperator transactions;
    private final Validator validator;

    public ProcedureService(
            ProcedureRepository repository, TransactionalOperator transactions, Validator validator) {
        this.repository = repository;
        this.transactions = transactions;
        this.validator = validator;
    }

    public Mono<DentalProcedureResponse> create(DentalProcedureRequest request) {
        var name = request.name().trim();
        return repository.countNameExcept(name, new UUID(0, 0))
                .flatMap(count -> {
                    if (count > 0) {
                        return Mono.error(new ConflictException("Procedure name is already in use"));
                    }
                    var procedure = ProcedureAccount.create();
                    apply(procedure, request);
                    validateEntity(procedure);
                    return repository.save(procedure).map(this::toResponse);
                })
                .as(transactions::transactional);
    }

    public Mono<DentalProcedureResponse> findById(UUID id) {
        return require(id).map(this::toResponse).as(transactions::transactional);
    }

    public Mono<ProcedurePage> findAll(int page, int size) {
        long offset = (long) page * size;
        return repository.findPage(size, offset)
                .map(this::toResponse)
                .collectList()
                .zipWith(repository.countAll())
                .map(tuple -> new ProcedurePage(tuple.getT1(), tuple.getT2()))
                .as(transactions::transactional);
    }

    public Mono<DentalProcedureResponse> update(UUID id, DentalProcedureRequest request) {
        var name = request.name().trim();
        return require(id).flatMap(procedure -> repository.countNameExcept(name, procedure.getId())
                        .flatMap(count -> {
                            if (count > 0) {
                                return Mono.error(new ConflictException("Procedure name is already in use"));
                            }
                            apply(procedure, request);
                            validateEntity(procedure);
                            return repository.save(procedure).map(this::toResponse);
                        }))
                .as(transactions::transactional);
    }

    public Mono<Void> deactivate(UUID id) {
        return require(id)
                .flatMap(procedure -> {
                    procedure.setActive(false);
                    return repository.save(procedure);
                })
                .then()
                .as(transactions::transactional);
    }

    private Mono<ProcedureAccount> require(UUID id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Dental procedure not found: " + id)));
    }

    private void apply(ProcedureAccount procedure, DentalProcedureRequest request) {
        procedure.setName(request.name().trim());
        procedure.setDescription(request.description() == null ? null : request.description().trim());
        procedure.setBasePrice(request.basePrice());
        procedure.setDurationMinutes(request.durationMinutes());
    }

    private void validateEntity(ProcedureAccount procedure) {
        var violations = validator.validate(procedure);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private DentalProcedureResponse toResponse(ProcedureAccount procedure) {
        return new DentalProcedureResponse(
                procedure.getId(),
                procedure.getName(),
                procedure.getDescription(),
                procedure.getBasePrice(),
                procedure.getDurationMinutes(),
                procedure.isActive());
    }

    public record ProcedurePage(java.util.List<DentalProcedureResponse> items, long total) {
    }
}
