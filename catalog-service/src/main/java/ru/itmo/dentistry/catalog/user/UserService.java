package ru.itmo.dentistry.catalog.user;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.itmo.dentistry.catalog.common.BusinessValidationException;
import ru.itmo.dentistry.catalog.common.ConflictException;
import ru.itmo.dentistry.catalog.common.NotFoundException;
import ru.itmo.dentistry.catalog.internal.CatalogUserView;
import ru.itmo.dentistry.catalog.procedure.ProcedureRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ProcedureRepository procedureRepository;
    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactions;
    private final Validator validator;

    public UserService(
            UserRepository userRepository,
            ProcedureRepository procedureRepository,
            DatabaseClient databaseClient,
            TransactionalOperator transactions,
            Validator validator) {
        this.userRepository = userRepository;
        this.procedureRepository = procedureRepository;
        this.databaseClient = databaseClient;
        this.transactions = transactions;
        this.validator = validator;
    }

    public Mono<UserResponse> create(UserRequest request) {
        validateDoctorFields(request);
        var email = normalizeEmail(request.email());
        var phone = request.phone().trim();
        return userRepository.countEmailExcept(email, new UUID(0, 0))
                .zipWith(userRepository.countPhoneExcept(phone, new UUID(0, 0)))
                .flatMap(counts -> {
                    if (counts.getT1() > 0) {
                        return Mono.error(new ConflictException("Email is already in use"));
                    }
                    if (counts.getT2() > 0) {
                        return Mono.error(new ConflictException("Phone is already in use"));
                    }
                    var user = UserAccount.create();
                    apply(user, request);
                    validateEntity(user);
                    return userRepository.save(user)
                            .flatMap(saved -> replaceSpecializations(saved.getId(), specializationsOf(request))
                                    .then(clearProceduresIfNeeded(saved.getId(), request.role()))
                                    .then(toResponse(saved)));
                })
                .as(transactions::transactional);
    }

    public Mono<UserResponse> findById(UUID id) {
        return require(id).flatMap(this::toResponse).as(transactions::transactional);
    }

    public Mono<CatalogUserView> findInternalById(UUID id) {
        return require(id).flatMap(user -> procedureIds(user.getId()).map(ids -> new CatalogUserView(
                user.getId(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.getWorkDayStart(),
                user.getWorkDayEnd(),
                ids))).as(transactions::transactional);
    }

    public Mono<Page<UserResponse>> findAll(int page, int size, UserRole role) {
        long offset = (long) page * size;
        Flux<UserAccount> rows = role == null
                ? userRepository.findPage(size, offset)
                : userRepository.findPageByRole(role.name(), size, offset);
        Mono<Long> total = role == null ? userRepository.countUsers() : userRepository.countByRole(role.name());
        return rows.concatMap(this::toResponse)
                .collectList()
                .zipWith(total)
                .map(tuple -> (Page<UserResponse>) new PageImpl<>(
                        tuple.getT1(),
                        PageRequest.of(page, size, Sort.by("fullName").ascending()),
                        tuple.getT2()))
                .as(transactions::transactional);
    }

    public Mono<UserResponse> update(UUID id, UserRequest request) {
        validateDoctorFields(request);
        var email = normalizeEmail(request.email());
        var phone = request.phone().trim();
        return require(id).flatMap(user -> userRepository.countEmailExcept(email, user.getId())
                        .zipWith(userRepository.countPhoneExcept(phone, user.getId()))
                        .flatMap(counts -> {
                            if (counts.getT1() > 0) {
                                return Mono.error(new ConflictException("Email is already in use"));
                            }
                            if (counts.getT2() > 0) {
                                return Mono.error(new ConflictException("Phone is already in use"));
                            }
                            apply(user, request);
                            validateEntity(user);
                            return userRepository.save(user)
                                    .flatMap(saved -> replaceSpecializations(saved.getId(), specializationsOf(request))
                                            .then(clearProceduresIfNeeded(saved.getId(), request.role()))
                                            .then(toResponse(saved)));
                        }))
                .as(transactions::transactional);
    }

    public Mono<Void> deactivate(UUID id) {
        return require(id)
                .flatMap(user -> {
                    user.setActive(false);
                    return userRepository.save(user);
                })
                .then()
                .as(transactions::transactional);
    }

    public Mono<UserResponse> assignProcedure(UUID doctorId, UUID procedureId) {
        return require(doctorId).flatMap(doctor -> {
            if (!UserRole.DOCTOR.name().equals(doctor.getRole())) {
                return Mono.error(new BusinessValidationException("User is not a doctor"));
            }
            return procedureRepository.findById(procedureId)
                    .switchIfEmpty(Mono.error(new NotFoundException("Dental procedure not found: " + procedureId)))
                    .flatMap(procedure -> {
                        if (!procedure.isActive()) {
                            return Mono.error(new BusinessValidationException(
                                    "Inactive procedure cannot be assigned to a doctor"));
                        }
                        return countDoctorProcedure(doctorId, procedureId)
                                .flatMap(count -> count > 0
                                        ? Mono.empty()
                                        : insertDoctorProcedure(doctorId, procedureId))
                                .then(toResponse(doctor));
                    });
        }).as(transactions::transactional);
    }

    public Mono<UserResponse> removeProcedure(UUID doctorId, UUID procedureId) {
        return require(doctorId).flatMap(doctor -> {
            if (!UserRole.DOCTOR.name().equals(doctor.getRole())) {
                return Mono.error(new BusinessValidationException("User is not a doctor"));
            }
            return databaseClient.sql("""
                            DELETE FROM doctor_procedures
                            WHERE doctor_id = :doctorId AND procedure_id = :procedureId
                            """)
                    .bind("doctorId", doctorId)
                    .bind("procedureId", procedureId)
                    .fetch()
                    .rowsUpdated()
                    .then(toResponse(doctor));
        }).as(transactions::transactional);
    }

    private Mono<UserAccount> require(UUID id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("User not found: " + id)));
    }

    private void apply(UserAccount user, UserRequest request) {
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone().trim());
        user.setEmail(normalizeEmail(request.email()));
        user.setRole(request.role().name());
        if (request.role() == UserRole.DOCTOR) {
            user.setWorkDayStart(request.workDayStart());
            user.setWorkDayEnd(request.workDayEnd());
        } else {
            user.setWorkDayStart(null);
            user.setWorkDayEnd(null);
        }
    }

    private void validateDoctorFields(UserRequest request) {
        boolean hasDoctorFields = request.workDayStart() != null || request.workDayEnd() != null
                || (request.specializations() != null && !request.specializations().isEmpty());
        if (request.role() != UserRole.DOCTOR && hasDoctorFields) {
            throw new BusinessValidationException("Only a doctor may have specializations and working hours");
        }
        if (request.role() == UserRole.DOCTOR) {
            if (request.workDayStart() == null || request.workDayEnd() == null
                    || !request.workDayStart().isBefore(request.workDayEnd())) {
                throw new BusinessValidationException("A doctor must have valid working hours");
            }
            if (request.specializations() == null || request.specializations().isEmpty()) {
                throw new BusinessValidationException("A doctor must have at least one specialization");
            }
        }
    }

    private void validateEntity(UserAccount user) {
        var violations = validator.validate(user);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private Set<DoctorSpecialization> specializationsOf(UserRequest request) {
        if (request.role() != UserRole.DOCTOR || request.specializations() == null) {
            return Set.of();
        }
        return Set.copyOf(request.specializations());
    }

    private Mono<Void> replaceSpecializations(UUID userId, Set<DoctorSpecialization> specializations) {
        return databaseClient.sql("DELETE FROM user_specializations WHERE user_id = :userId")
                .bind("userId", userId)
                .fetch()
                .rowsUpdated()
                .thenMany(Flux.fromIterable(specializations))
                .concatMap(specialization -> databaseClient.sql("""
                                INSERT INTO user_specializations (user_id, specialization)
                                VALUES (:userId, :specialization)
                                """)
                        .bind("userId", userId)
                        .bind("specialization", specialization.name())
                        .fetch()
                        .rowsUpdated())
                .then();
    }

    private Mono<Void> clearProceduresIfNeeded(UUID userId, UserRole role) {
        if (role == UserRole.DOCTOR) {
            return Mono.empty();
        }
        return databaseClient.sql("DELETE FROM doctor_procedures WHERE doctor_id = :doctorId")
                .bind("doctorId", userId)
                .fetch()
                .rowsUpdated()
                .then();
    }

    private Mono<Long> countDoctorProcedure(UUID doctorId, UUID procedureId) {
        return databaseClient.sql("""
                        SELECT COUNT(*) AS total FROM doctor_procedures
                        WHERE doctor_id = :doctorId AND procedure_id = :procedureId
                        """)
                .bind("doctorId", doctorId)
                .bind("procedureId", procedureId)
                .map(row -> row.get("total", Long.class))
                .one();
    }

    private Mono<Void> insertDoctorProcedure(UUID doctorId, UUID procedureId) {
        return databaseClient.sql("""
                        INSERT INTO doctor_procedures (doctor_id, procedure_id)
                        VALUES (:doctorId, :procedureId)
                        """)
                .bind("doctorId", doctorId)
                .bind("procedureId", procedureId)
                .fetch()
                .rowsUpdated()
                .then();
    }

    private Mono<UserResponse> toResponse(UserAccount user) {
        return specializations(user.getId()).zipWith(procedureIds(user.getId()))
                .map(tuple -> new UserResponse(
                        user.getId(),
                        user.getFullName(),
                        user.getPhone(),
                        user.getEmail(),
                        UserRole.valueOf(user.getRole()),
                        user.isActive(),
                        tuple.getT1(),
                        user.getWorkDayStart(),
                        user.getWorkDayEnd(),
                        tuple.getT2()));
    }

    private Mono<Set<DoctorSpecialization>> specializations(UUID userId) {
        return databaseClient.sql("SELECT specialization FROM user_specializations WHERE user_id = :userId")
                .bind("userId", userId)
                .map(row -> DoctorSpecialization.valueOf(row.get("specialization", String.class)))
                .all()
                .collectList()
                .map(HashSet::new);
    }

    private Mono<Set<UUID>> procedureIds(UUID userId) {
        return databaseClient.sql("SELECT procedure_id FROM doctor_procedures WHERE doctor_id = :doctorId")
                .bind("doctorId", userId)
                .map(row -> row.get("procedure_id", UUID.class))
                .all()
                .collectList()
                .map(HashSet::new);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
