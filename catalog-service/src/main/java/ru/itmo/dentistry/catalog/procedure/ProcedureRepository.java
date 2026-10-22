package ru.itmo.dentistry.catalog.procedure;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProcedureRepository extends ReactiveCrudRepository<ProcedureAccount, UUID> {

    @Query("SELECT * FROM dental_procedures ORDER BY name ASC LIMIT :limit OFFSET :offset")
    Flux<ProcedureAccount> findPage(@Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT COUNT(*) FROM dental_procedures")
    Mono<Long> countAll();

    @Query("SELECT COUNT(*) FROM dental_procedures WHERE lower(name) = lower(:name) AND id <> :id")
    Mono<Long> countNameExcept(@Param("name") String name, @Param("id") UUID id);
}
