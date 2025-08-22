package org.qpeek.qpeek.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // TODO: SecurityContext 기반 AuditorAware 적용 예정.
}