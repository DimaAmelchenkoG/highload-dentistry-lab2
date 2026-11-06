package ru.itmo.dentistry.catalog.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.FetchSpec;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.itmo.dentistry.catalog.common.BusinessValidationException;
import ru.itmo.dentistry.catalog.common.ConflictException;
import ru.itmo.dentistry.catalog.common.NotFoundException;
import ru.itmo.dentistry.catalog.procedure.ProcedureRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProcedureRepository procedureRepository;

    @Mock
    private DatabaseClient databaseClient;

    @Mock
    private TransactionalOperator transactions;

    private UserService service;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        stubDatabase();
        when(transactions.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(UserAccount.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        service = new UserService(userRepository, procedureRepository, databaseClient, transactions, validator);
    }

    @Test
    void createsPatientAndNormalizesEmail() {
        when(userRepository.countByEmailIgnoreCaseAndIdNot(anyString(), any())).thenReturn(Mono.just(0L));
        when(userRepository.countByPhoneAndIdNot(anyString(), any())).thenReturn(Mono.just(0L));

        StepVerifier.create(service.create(patient("  Ivan Petrov  ", " Ivan@Example.com ")))
                .assertNext(response -> {
                    assertEquals("Ivan Petrov", response.fullName());
                    assertEquals("ivan@example.com", response.email());
                    assertEquals(UserRole.PATIENT, response.role());
                })
                .verifyComplete();
    }

    @Test
    void rejectsDuplicateEmail() {
        when(userRepository.countByEmailIgnoreCaseAndIdNot(anyString(), any())).thenReturn(Mono.just(1L));
        when(userRepository.countByPhoneAndIdNot(anyString(), any())).thenReturn(Mono.just(0L));

        StepVerifier.create(service.create(patient("Ivan Petrov", "ivan@example.com")))
                .expectError(ConflictException.class)
                .verify();
    }

    @Test
    void rejectsDoctorFieldsOnPatient() {
        var request = new UserRequest(
                "Ivan Petrov",
                "+79990000000",
                "ivan@example.com",
                UserRole.PATIENT,
                Set.of(DoctorSpecialization.THERAPIST),
                null,
                null);

        assertThrows(BusinessValidationException.class, () -> service.create(request));
    }

    @Test
    void rejectsDoctorWithoutSpecializations() {
        var request = new UserRequest(
                "Anna Smirnova",
                "+79990000001",
                "anna@example.com",
                UserRole.DOCTOR,
                Set.of(),
                LocalTime.of(9, 0),
                LocalTime.of(18, 0));

        assertThrows(BusinessValidationException.class, () -> service.create(request));
    }

    @Test
    void deactivatesUser() {
        UserAccount user = UserAccount.create();
        user.setFullName("Ivan Petrov");
        user.setPhone("+79990000000");
        user.setEmail("ivan@example.com");
        user.setRole(UserRole.PATIENT.name());
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));

        StepVerifier.create(service.deactivate(user.getId())).verifyComplete();

        assertFalse(user.isActive());
        verify(userRepository).save(user);
    }

    @Test
    void deletesUserPermanently() {
        UserAccount user = UserAccount.create();
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));
        when(userRepository.delete(user)).thenReturn(Mono.empty());

        StepVerifier.create(service.deletePermanently(user.getId())).verifyComplete();

        verify(userRepository).delete(user);
    }

    @Test
    void missingUserCannotBeDeleted() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(service.deletePermanently(id))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void onlyDoctorCanReceiveProcedure() {
        UserAccount patient = UserAccount.create();
        patient.setRole(UserRole.PATIENT.name());
        when(userRepository.findById(patient.getId())).thenReturn(Mono.just(patient));

        StepVerifier.create(service.assignProcedure(patient.getId(), UUID.randomUUID()))
                .expectError(BusinessValidationException.class)
                .verify();
    }

    private static UserRequest patient(String fullName, String email) {
        return new UserRequest(fullName, "+79990000000", email, UserRole.PATIENT, null, null, null);
    }

    @SuppressWarnings("unchecked")
    private void stubDatabase() {
        DatabaseClient.GenericExecuteSpec spec = mock(DatabaseClient.GenericExecuteSpec.class);
        FetchSpec<java.util.Map<String, Object>> fetch = mock(FetchSpec.class);
        RowsFetchSpec<Object> rows = mock(RowsFetchSpec.class);
        when(databaseClient.sql(anyString())).thenReturn(spec);
        when(spec.bind(anyString(), any())).thenReturn(spec);
        when(spec.fetch()).thenReturn(fetch);
        when(fetch.rowsUpdated()).thenReturn(Mono.just(0L));
        when(spec.map(any(Function.class))).thenReturn(rows);
        when(rows.all()).thenReturn(Flux.empty());
    }
}
