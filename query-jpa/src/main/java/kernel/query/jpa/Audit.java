package kernel.query.jpa;

import jakarta.annotation.Nullable;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record Audit(
        @Nullable UUID userId,
        @Nullable String username,
        @Nullable UUID permissionGroupId,
        @Nullable String permissionGroup
) {
    public static final Audit SYSTEM = new Audit(null, "system", null, "SYSTEM");
}