package org.qpeek.qpeek.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity extends BaseTimeEntity {

    @CreatedBy
    @Column(name = "created_by", updatable = false) // TODO: AuditAware 구성 이후 NOT NULL 승격 필요
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by") // TODO: AuditAware 구성 이후 NOT NULL 승격 필요
    private String lastModifiedBy;
}
