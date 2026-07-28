package com.gangku.be.config.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, Object> t = new RedisTemplate<>();
        t.setConnectionFactory(cf);
        var keySer = new StringRedisSerializer();

        // LocalDateTime 등 java.time 타입을 캐시 대상에 담을 수 있도록 jsr310 모듈을 등록한 매퍼를 사용한다.
        // objectMapper()로 커스텀 매퍼를 넘기면 @class 타입 정보 주입이 기본으로 꺼지므로 defaultTyping(true)로
        // 명시적으로 켜야 HomeCache.getOrFetch의 제네릭 역직렬화가 계속 동작한다.
        ObjectMapper redisObjectMapper =
                new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var valSer =
                GenericJackson2JsonRedisSerializer.builder()
                        .objectMapper(redisObjectMapper)
                        .defaultTyping(true)
                        .build();
        t.setKeySerializer(keySer);
        t.setHashKeySerializer(keySer);
        t.setValueSerializer(valSer);
        t.setHashValueSerializer(valSer);
        t.afterPropertiesSet();
        return t;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }
}
