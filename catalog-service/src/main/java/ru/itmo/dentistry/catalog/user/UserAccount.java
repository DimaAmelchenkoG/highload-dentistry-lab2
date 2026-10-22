package ru.itmo.dentistry.catalog.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
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
@Table("users")
public class UserAccount implements Persistable<UUID> {

    @Id
    @Setter(AccessLevel.NONE)
    private UUID id;

    @NotBlank
    @Size(max = 200)
    @Column("full_name")
    private String fullName;

    @NotBlank
    @Size(max = 32)
    private String phone;

    @NotBlank
    @Email
    @Size(max = 320)
    private String email;

    @NotNull
    @Size(max = 32)
    private String role;

    private boolean active = true;

    @Column("work_day_start")
    private LocalTime workDayStart;

    @Column("work_day_end")
    private LocalTime workDayEnd;

    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean newEntity;

    public static UserAccount create() {
        var user = new UserAccount();
        user.id = UUID.randomUUID();
        user.newEntity = true;
        user.active = true;
        return user;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public void markPersisted() {
        this.newEntity = false;
    }
}
