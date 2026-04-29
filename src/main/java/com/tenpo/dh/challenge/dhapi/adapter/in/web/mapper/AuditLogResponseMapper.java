package com.tenpo.dh.challenge.dhapi.adapter.in.web.mapper;

import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.AuditLogDetailResponse;
import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.AuditLogSummaryResponse;
import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.LinkDto;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import org.mapstruct.Mapper;

import java.util.Map;

/**
 * MapStruct mapper that converts an {@link AuditLog} domain object into response DTOs.
 */
@Mapper(componentModel = "spring")
public interface AuditLogResponseMapper {

    AuditLogDetailResponse toDetailResponse(AuditLog auditLog);

    default AuditLogSummaryResponse toSummaryResponse(AuditLog auditLog, String detailHref) {
        return new AuditLogSummaryResponse(
                auditLog.getId(),
                auditLog.getCreatedAt(),
                auditLog.getAction(),
                auditLog.getActionType(),
                auditLog.getCallDirection(),
                auditLog.getUserId(),
                auditLog.getTransactionalId(),
                auditLog.getMethod(),
                auditLog.getEndpoint(),
                auditLog.getParams(),
                auditLog.getStatusCode(),
                auditLog.getErrorMessage(),
                auditLog.getDurationMs(),
                Map.of("detail", new LinkDto(detailHref))
        );
    }
}
