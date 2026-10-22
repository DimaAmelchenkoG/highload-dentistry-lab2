package ru.itmo.dentistry.catalog.procedure;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table("dental_procedures")
public class ProcedureAccount implements Persistable<UUID> {

    @Id
    @Setter(AccessLevel.NONE)
    private UUID id;

    @NotBlank
    @Size(max = 160)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull
    @DecimalMin("0.00")
    @Column("base_price")
    private BigDecimal basePrice;

    @NotNull
    @Positive
    @Column("duration_minutes")
    private Integer durationMinutes;

    private boolean active = true;

    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean newEntity;

    public static ProcedureAccount create() {
        var procedure = new ProcedureAccount();
        procedure.id = UUID.randomUUID();
        procedure.newEntity = true;
        procedure.active = true;
        return procedure;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public void markPersisted() {
        this.newEntity = false;
    }
}
