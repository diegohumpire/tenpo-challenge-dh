package com.tenpo.dh.challenge.dhapi.adapter.in.web.dto;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper that converts an {@link AuditLog} domain object into an
 * {@link AuditLogResponse} DTO.
 */
@Mapper(componentModel = "spring")
public interface AuditLogResponseMapper {
    AuditLogResponse toResponse(AuditLog auditLog);
}
