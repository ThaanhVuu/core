package kernel.query.jpa;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Bật JPA auditing cho mọi service dùng kernel, và đăng ký {@link SecurityAuditorAware} làm
 * {@link AuditorAware} mặc định.
 *
 * <p>Đăng ký qua
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 * Service nào cần cách lấy user khác chỉ việc khai báo bean tên {@code auditorAware} của riêng
 * mình ({@link ConditionalOnMissingBean} sẽ nhường chỗ).
 *
 * <p>Cần dependency {@code spring-boot-starter-oauth2-resource-server} (hoặc
 * {@code spring-security-oauth2-jose}) có mặt ở runtime của service.
 */
@AutoConfiguration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    @ConditionalOnMissingBean(name = "auditorAware")
    public AuditorAware<Audit> auditorAware() {
        return new SecurityAuditorAware();
    }
}