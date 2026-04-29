package com.tenpo.dh.challenge.dhapi.adapter.out.persistence;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class R2dbcAuditLogRepositoryTest {

    @Mock
    private AuditLogR2dbcDao dao;

    @InjectMocks
    private R2dbcAuditLogRepository repository;

    @Test
    void save_delegatesToDao() {
        AuditLog log = AuditLog.builder().id(1L).action("TEST").build();
        when(dao.save(log)).thenReturn(Mono.just(log));

        StepVerifier.create(repository.save(log))
                .expectNext(log)
                .verifyComplete();
    }

    @Test
    void findAll_returnsPaginatedResultsWithCorrectMetadata() {
        AuditLog log = AuditLog.builder().id(1L).action("TEST").build();
        PageRequest pageable = PageRequest.of(0, 10);

        // total must be >= offset + pageSize to prevent PageImpl from correcting it
        // (Spring Data uses offset+content.size() when offset+pageSize > total)
        when(dao.count()).thenReturn(Mono.just(100L));
        when(dao.findAllBy(pageable)).thenReturn(Flux.fromIterable(List.of(log)));

        StepVerifier.create(repository.findAll(pageable))
                .assertNext(page -> {
                    assertThat(page.getTotalElements()).isEqualTo(100L);
                    assertThat(page.getContent()).containsExactly(log);
                    assertThat(page.getSize()).isEqualTo(10);
                })
                .verifyComplete();
    }
}
