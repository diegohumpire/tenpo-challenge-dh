package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper that converts a {@link WebExchangeAuditContext} into an
 * {@link AuditLog} domain object.
 *
 * <p>Business rules for {@code action} and {@code actionType} are encapsulated
 * here, keeping {@link AuditLogFilter} focused solely on data extraction.
 */
@Mapper(componentModel = "spring")
public interface WebExchangeAuditLogMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "action", expression = "java(resolveAction(context))")
    @Mapping(target = "actionType", expression = "java(resolveActionType(context.path()))")
    @Mapping(target = "callDirection", constant = "IN")
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "responseHeaders", ignore = true)
    @Mapping(target = "responseBody", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(source = "path", target = "endpoint")
    AuditLog toAuditLog(WebExchangeAuditContext context);

    default String resolveAction(WebExchangeAuditContext context) {
        String path = context.path();
        String method = context.method();
        if (path.contains("/calculations")) return "CREATE_CALCULATION";
        if (path.contains("/audit-logs")) return "GET_AUDIT_LOGS";
        return method + "_" + path.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();
    }

    default AuditActionType resolveActionType(String path) {
        if (path.contains("/calculations")) return AuditActionType.CALCULATION;
        return AuditActionType.HTTP_REQUEST;
    }
}
