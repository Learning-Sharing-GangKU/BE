package com.gangku.be.dto.gathering.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.gangku.be.config.redis.RedisConfig;
import com.gangku.be.model.common.PageMeta;
import com.gangku.be.model.gathering.GatheringListItem;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * 실제 운영에서 쓰는 RedisConfig의 값 직렬화기로 GatheringListResponseDto를 serialize → deserialize 했을 때 record 타입
 * (GatheringListItem, PageMeta)과 LocalDateTime 필드가 원본과 동일하게 복원되는지 검증한다.
 *
 * <p>Redis 없이 순수 인메모리로 실행 — 인프라 의존 없음.
 */
@Tag("unit")
class GatheringListResponseDtoSerializationTest {

    @SuppressWarnings("unchecked")
    private final RedisSerializer<Object> serializer =
            (RedisSerializer<Object>)
                    new RedisConfig()
                            .redisTemplate(mock(RedisConnectionFactory.class))
                            .getValueSerializer();

    @Test
    @DisplayName("GatheringListResponseDto (record 내포) 직렬화/역직렬화 라운드트립 성공")
    void roundtrip_gatheringListResponseDto() {
        // given
        LocalDateTime gatheringDate = LocalDateTime.of(2026, 4, 1, 19, 0);
        GatheringListItem item =
                new GatheringListItem(
                        "gathering_1",
                        "https://cdn.example.com/image.jpg",
                        "스터디",
                        "알고리즘 스터디",
                        "함께 코딩 실력을 키워요",
                        "서울 강남구",
                        gatheringDate);

        PageMeta meta =
                new PageMeta(
                        1, // page (1-base)
                        3, // size
                        10L, // totalElements
                        4, // totalPages
                        "createdAt,desc,id,desc",
                        false, // hasPrev
                        true // hasNext
                        );

        GatheringListResponseDto original =
                GatheringListResponseDto.builder().data(List.of(item)).meta(meta).build();

        // when
        byte[] bytes = serializer.serialize(original);
        Object deserialized = serializer.deserialize(bytes);

        // then
        assertThat(deserialized).isInstanceOf(GatheringListResponseDto.class);

        GatheringListResponseDto restored = (GatheringListResponseDto) deserialized;
        assertThat(restored.getData()).hasSize(1);
        assertThat(restored.getMeta().page()).isEqualTo(1);
        assertThat(restored.getMeta().size()).isEqualTo(3);
        assertThat(restored.getMeta().totalElements()).isEqualTo(10L);
        assertThat(restored.getMeta().sortedBy()).isEqualTo("createdAt,desc,id,desc");
        assertThat(restored.getMeta().hasPrev()).isFalse();
        assertThat(restored.getMeta().hasNext()).isTrue();

        GatheringListItem restoredItem = restored.getData().get(0);
        assertThat(restoredItem.id()).isEqualTo("gathering_1");
        assertThat(restoredItem.category()).isEqualTo("스터디");
        assertThat(restoredItem.title()).isEqualTo("알고리즘 스터디");
        assertThat(restoredItem.date()).isEqualTo(gatheringDate);
    }

    @Test
    @DisplayName("data가 빈 리스트인 GatheringListResponseDto 라운드트립 성공")
    void roundtrip_emptyData() {
        // given
        PageMeta meta = new PageMeta(1, 3, 0L, 0, "createdAt,desc,id,desc", false, false);
        GatheringListResponseDto original =
                GatheringListResponseDto.builder().data(List.of()).meta(meta).build();

        // when
        byte[] bytes = serializer.serialize(original);
        Object deserialized = serializer.deserialize(bytes);

        // then
        assertThat(deserialized).isInstanceOf(GatheringListResponseDto.class);
        GatheringListResponseDto restored = (GatheringListResponseDto) deserialized;
        assertThat(restored.getData()).isEmpty();
        assertThat(restored.getMeta().totalElements()).isZero();
    }
}
