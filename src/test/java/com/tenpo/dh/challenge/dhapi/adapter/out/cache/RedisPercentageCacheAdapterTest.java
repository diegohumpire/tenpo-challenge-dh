package com.tenpo.dh.challenge.dhapi.adapter.out.cache;

import com.tenpo.dh.challenge.dhapi.config.PercentageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisPercentageCacheAdapterTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOps;

    private RedisPercentageCacheAdapter adapter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        PercentageProperties properties = new PercentageProperties();
        adapter = new RedisPercentageCacheAdapter(redisTemplate, properties);
    }

    @Test
    void get_valuePresent_returnsPercentage() {
        when(valueOps.get("percentage:current")).thenReturn(Mono.just("15.5"));

        StepVerifier.create(adapter.get())
                .expectNextMatches(v -> v.compareTo(new BigDecimal("15.5")) == 0)
                .verifyComplete();
    }

    @Test
    void get_noValue_returnsEmpty() {
        when(valueOps.get("percentage:current")).thenReturn(Mono.empty());

        StepVerifier.create(adapter.get())
                .verifyComplete();
    }

    @Test
    void put_validPercentage_storesWithTtl() {
        when(valueOps.set(eq("percentage:current"), eq("10"), any(Duration.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(adapter.put(BigDecimal.TEN))
                .verifyComplete();

        verify(valueOps).set(eq("percentage:current"), eq("10"), any(Duration.class));
    }

    @Test
    void clear_deletesKey() {
        when(redisTemplate.delete("percentage:current")).thenReturn(Mono.just(1L));

        StepVerifier.create(adapter.clear())
                .verifyComplete();

        verify(redisTemplate).delete("percentage:current");
    }
}
