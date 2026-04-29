package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Optional;

/**
 * MapStruct mapper que convierte un {@link WebExchangeAuditContext} en un
 * {@link AuditLog} domain object.
 *
 * <p>Las reglas de negocio para {@code action} y {@code actionType} están encapsuladas
 * aquí, manteniendo {@link AuditLogFilter} enfocado solo en la extracción de datos.
 */
@Mapper(componentModel = "spring")
public interface WebExchangeAuditLogMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "action", expression = "java(resolveAction(context))")
    @Mapping(target = "actionType", expression = "java(resolveActionType(context.path()))")
    @Mapping(target = "callDirection", constant = "IN")
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(source = "path", target = "endpoint")
    AuditLog toAuditLog(WebExchangeAuditContext context);

    default String resolveAction(WebExchangeAuditContext context) {
        String path = Optional.ofNullable(context.path()).orElse("");
        String method = Optional.ofNullable(context.method()).orElse("UNKNOWN");
        if (path.contains("/calculations")) return "CREATE_CALCULATION";
        if (path.contains("/audit-logs")) return "GET_AUDIT_LOGS";
        return method + "_" + path.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();
    }

    default AuditActionType resolveActionType(String path) {
        String safePath = Optional.ofNullable(path).orElse("");
        if (safePath.contains("/calculations")) return AuditActionType.CALCULATION;
        return AuditActionType.HTTP_REQUEST;
    }
}
