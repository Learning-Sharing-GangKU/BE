package com.gangku.be.util.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class HomeCacheTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks private HomeCache homeCache;

    @Test
    @DisplayName("getOrFetch - 캐시 미스: fetcher 1회 실행 + Redis set 호출")
    void getOrFetch_cacheMiss_fetcherExecutedAndResultStored() {
        // given
        String key = "home:latest:p1:s3";
        Duration ttl = Duration.ofMinutes(10);
        String expected = "fresh_data";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(null);

        // when
        String result = homeCache.getOrFetch(key, ttl, () -> expected);

        // then
        assertThat(result).isEqualTo(expected);
        verify(valueOperations, times(1)).get(key);
        verify(valueOperations, times(1)).set(key, expected, ttl);
    }

    @Test
    @DisplayName("getOrFetch - 캐시 히트: fetcher 미호출, 캐시값 그대로 반환")
    void getOrFetch_cacheHit_fetcherNeverCalled() {
        // given
        String key = "home:popular:p1:s3";
        Duration ttl = Duration.ofMinutes(10);
        String cached = "cached_data";
        AtomicInteger callCount = new AtomicInteger(0);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(cached);

        // when
        String result =
                homeCache.getOrFetch(
                        key,
                        ttl,
                        () -> {
                            callCount.incrementAndGet();
                            return "should_not_be_called";
                        });

        // then
        assertThat(result).isEqualTo(cached);
        assertThat(callCount.get()).isZero();
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("invalidateHome - latest/popular/recommend 패턴의 키가 모두 삭제됨")
    void invalidateHome_deletesAllHomeKeys() {
        // given
        Set<String> latestKeys = Set.of("home:latest:p1:s3");
        Set<String> popularKeys = Set.of("home:popular:p1:s3");
        Set<String> recommendKeys =
                Set.of("home:recommend:123:p1:s3", "home:recommend:anonymous:p1:s3");

        when(redisTemplate.keys("home:latest:*")).thenReturn(latestKeys);
        when(redisTemplate.keys("home:popular:*")).thenReturn(popularKeys);
        when(redisTemplate.keys("home:recommend:*")).thenReturn(recommendKeys);

        // when
        homeCache.invalidateHome();

        // then
        verify(redisTemplate, times(1)).delete(latestKeys);
        verify(redisTemplate, times(1)).delete(popularKeys);
        verify(redisTemplate, times(1)).delete(recommendKeys);
    }

    @Test
    @DisplayName("invalidateHome - 해당 패턴 키가 없으면 delete 미호출")
    void invalidateHome_emptyKeys_deleteNotCalled() {
        // given
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());

        // when
        homeCache.invalidateHome();

        // then
        verify(redisTemplate, never()).delete(anyCollection());
    }
}
