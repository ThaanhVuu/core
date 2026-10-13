package kernel.query.jpa;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

@AutoConfiguration
@EnableJpaAuditing
public class JpaAuditingConfig {
    static final String CLAIM_USERNAME = "preferred_username";
    static final String CLAIM_GROUP_ID = "permission_group_id";
    static final String CLAIM_GROUP = "permission_group";

    @Bean
    @ConditionalOnMissingBean(name = "auditorAware")
    public AuditorAware<Audit> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(Jwt.class::isInstance)
                .map(Jwt.class::cast)
                .map(JpaAuditingConfig::toAudit)
                .or(() -> Optional.of(Audit.SYSTEM));
    }

    private static Audit toAudit(Jwt jwt) {
        return new Audit(
                parseUuid(jwt.getSubject()),
                jwt.getClaimAsString(CLAIM_USERNAME),
                parseUuid(jwt.getClaimAsString(CLAIM_GROUP_ID)),
                jwt.getClaimAsString(CLAIM_GROUP));
    }

    /** Trả {@code null} thay vì ném exception khi claim thiếu hoặc không phải UUID. */
    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
