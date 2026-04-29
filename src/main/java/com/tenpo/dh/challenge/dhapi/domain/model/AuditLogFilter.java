package com.tenpo.dh.challenge.dhapi.domain.model;

import java.util.List;

public record AuditLogFilter(
        String userId,
        String transactionalId,
        String action,
        AuditActionType actionType,
        CallDirection callDirection,
        List<String> excludeActions
) {

    public static AuditLogFilter empty() {
        return new AuditLogFilter(null, null, null, null, null, List.of());
    }

    public static AuditLogFilter forTransactionalId(String transactionalId) {
        return new AuditLogFilter(null, transactionalId, null, null, null, List.of("GET_AUDIT_LOGS"));
    }

    public static AuditLogFilter forUserId(String userId) {
        return new AuditLogFilter(userId, null, null, null, null, List.of("GET_AUDIT_LOGS"));
    }

    public boolean isEmpty() {
        return userId == null && transactionalId == null && action == null
                && actionType == null && callDirection == null && excludeActions.isEmpty();
    }
}
