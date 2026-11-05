package ru.itmo.dentistry.catalog.procedure;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProcedureRepository extends ReactiveCrudRepository<ProcedureAccount, UUID> {

    Flux<ProcedureAccount> findBy(Pageable pageable);

    Mono<Long> countByNameIgnoreCaseAndIdNot(String name, UUID id);
}
