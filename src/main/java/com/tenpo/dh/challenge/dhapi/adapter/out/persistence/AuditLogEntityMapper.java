package com.tenpo.dh.challenge.dhapi.adapter.out.persistence;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogEntityMapper {
    AuditLog toDomain(AuditLogEntity entity);
    AuditLogEntity toEntity(AuditLog domain);
}
