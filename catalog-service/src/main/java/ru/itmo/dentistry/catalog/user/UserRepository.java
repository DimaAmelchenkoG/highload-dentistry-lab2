package ru.itmo.dentistry.catalog.user;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<UserAccount, UUID> {

    Flux<UserAccount> findBy(Pageable pageable);

    Flux<UserAccount> findByRole(String role, Pageable pageable);

    Mono<Long> countByRole(String role);

    Mono<Long> countByEmailIgnoreCaseAndIdNot(String email, UUID id);

    Mono<Long> countByPhoneAndIdNot(String phone, UUID id);
}
