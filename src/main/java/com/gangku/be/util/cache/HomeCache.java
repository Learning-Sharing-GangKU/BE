package com.gangku.be.util.cache;

import com.gangku.be.constant.cache.CacheKeys;
import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HomeCache {

    public static final Duration TTL_LATEST = Duration.ofMinutes(10);
    public static final Duration TTL_POPULAR = Duration.ofMinutes(10);
    public static final Duration TTL_RECOMMEND = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 캐시에서 값을 조회하고, 없으면 fetcher를 실행해 저장 후 반환한다.
     *
     * <p>GenericJackson2JsonRedisSerializer가 @class 타입 정보를 JSON에 포함하므로 역직렬화 시 T로 안전하게 복원된다.
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrFetch(String key, Duration ttl, Supplier<T> fetcher) {
        T cached = (T) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        T result = fetcher.get();
        redisTemplate.opsForValue().set(key, result, ttl);
        return result;
    }

    /**
     * 홈 관련 캐시를 모두 무효화한다 (latest / popular / recommend 패턴).
     *
     * <p>Note: keys()는 O(N)이지만 홈 캐시 키스페이스가 소규모(page=1 size=3 고정)라 허용한다.
     */
    public void invalidateHome() {
        deleteByPattern(CacheKeys.PREFIX_HOME_LATEST + "*");
        deleteByPattern(CacheKeys.PREFIX_HOME_POPULAR + "*");
        deleteByPattern(CacheKeys.PREFIX_HOME_RECOMMEND + "*");
    }

    private void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete((Collection<String>) keys);
        }
    }
}
