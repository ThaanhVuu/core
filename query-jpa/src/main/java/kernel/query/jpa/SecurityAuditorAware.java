package kernel.query.jpa;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

/**
 * Cung cấp "người đang thao tác" cho JPA auditing, lấy từ JWT của request hiện tại.
 *
 * <p>Trả về {@link Audit#SYSTEM} khi không có user: không có authentication (job nền, scheduler,
 * consumer, {@code @Async}), request ẩn danh, hoặc principal không phải {@link Jwt}.
 *
 * <p>Claim được đọc: {@code sub} (userId), {@code preferred_username}, {@code permission_group_id},
 * {@code permission_group}. Sửa các hằng bên dưới nếu identity-service đặt tên khác. Thiếu hoặc sai
 * định dạng claim thì field tương ứng là {@code null}, không ném lỗi.
 *
 * <p>Đăng ký bean ở {@link JpaAuditingConfig}, không dùng {@code @Component} vì service không
 * quét package của kernel.
 */
public class SecurityAuditorAware implements AuditorAware<Audit> {

    static final String CLAIM_USERNAME = "preferred_username";
    static final String CLAIM_GROUP_ID = "permission_group_id";
    static final String CLAIM_GROUP = "permission_group";

    @Override
    public Optional<Audit> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.of(Audit.SYSTEM);
        }

        return Optional.of(new Audit(
                parseUuid(jwt.getSubject()),
                jwt.getClaimAsString(CLAIM_USERNAME),
                parseUuid(jwt.getClaimAsString(CLAIM_GROUP_ID)),
                jwt.getClaimAsString(CLAIM_GROUP)));
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