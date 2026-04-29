package com.tenpo.dh.challenge.dhapi.application.service;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditRequestContext;
import com.tenpo.dh.challenge.dhapi.domain.model.Calculation;
import com.tenpo.dh.challenge.dhapi.domain.port.in.CalculationUseCase;
import com.tenpo.dh.challenge.dhapi.domain.port.in.PercentageResolverUseCase;
import com.tenpo.dh.challenge.dhapi.domain.port.out.AuditEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CalculationService implements CalculationUseCase {

    private final PercentageResolverUseCase percentageResolverUseCase;
    private final AuditEventPublisher auditEventPublisher;

    @Override
    public Mono<Calculation> calculate(BigDecimal num1, BigDecimal num2) {
        return Mono.deferContextual(ctx -> {
            AuditRequestContext auditCtx = ctx.getOrDefault(AuditRequestContext.class, AuditRequestContext.empty());
            return percentageResolverUseCase.resolvePercentage()
                    .map(pct -> Calculation.of(num1, num2, pct))
                    .doOnNext(calc -> publishCalculation(auditCtx, num1, num2, calc));
        });
    }

    private void publishCalculation(AuditRequestContext ctx, BigDecimal num1, BigDecimal num2, Calculation calc) {
        auditEventPublisher.publish(AuditLog.builder()
                .action("CALCULATE")
                .actionType(AuditActionType.SYSTEM)
                .transactionalId(ctx.transactionalId())
                .userId(ctx.userId())
                .requestBody("{\"num1\":\"" + num1.toPlainString() + "\",\"num2\":\"" + num2.toPlainString() + "\"}")
                .responseBody("{\"sum\":\"" + calc.sum().toPlainString() + "\",\"percentage\":\"" + calc.percentage().toPlainString() + "\",\"result\":\"" + calc.result().toPlainString() + "\"}")
                .build());
    }
}
