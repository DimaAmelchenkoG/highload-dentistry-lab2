package ru.itmo.dentistry.catalog.user;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<UserAccount, UUID> {

    @Query("SELECT * FROM users ORDER BY full_name ASC LIMIT :limit OFFSET :offset")
    Flux<UserAccount> findPage(@Param("limit") int limit, @Param("offset") long offset);

    @Query("""
            SELECT * FROM users
            WHERE role = :role
            ORDER BY full_name ASC
            LIMIT :limit OFFSET :offset
            """)
    Flux<UserAccount> findPageByRole(
            @Param("role") String role, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT COUNT(*) FROM users")
    Mono<Long> countUsers();

    @Query("SELECT COUNT(*) FROM users WHERE role = :role")
    Mono<Long> countByRole(@Param("role") String role);

    @Query("SELECT COUNT(*) FROM users WHERE lower(email) = lower(:email) AND id <> :id")
    Mono<Long> countEmailExcept(@Param("email") String email, @Param("id") UUID id);

    @Query("SELECT COUNT(*) FROM users WHERE phone = :phone AND id <> :id")
    Mono<Long> countPhoneExcept(@Param("phone") String phone, @Param("id") UUID id);
}
