package com.tenpo.dh.challenge.dhapi.adapter.out.cache;

import com.tenpo.dh.challenge.dhapi.domain.port.out.PercentageCacheStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPercentageCacheAdapter implements PercentageCacheStore {

    private static final String CACHE_KEY = "percentage:current";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Override
    public Mono<BigDecimal> get() {
        return redisTemplate.opsForValue()
                .get(CACHE_KEY)
                .map(BigDecimal::new)
                .doOnNext(v -> log.debug("Cache hit: percentage={}", v))
                .switchIfEmpty(Mono.fromRunnable(() -> log.debug("Cache miss: no percentage stored")));
    }

    @Override
    public Mono<Void> put(BigDecimal percentage) {
        return redisTemplate.opsForValue()
                .set(CACHE_KEY, percentage.toPlainString(), TTL)
                .doOnNext(ok -> log.debug("Cached percentage={} TTL=30min", percentage))
                .then();
    }
}
