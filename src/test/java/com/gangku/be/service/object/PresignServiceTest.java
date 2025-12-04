package com.gangku.be.service.object;

import com.gangku.be.config.aws.AssetPolicyProps;
import com.gangku.be.config.aws.AwsAppProps;
import com.gangku.be.dto.object.PresignRequestDto;
import com.gangku.be.dto.object.PresignResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.ObjectStorageErrorCode;
import com.gangku.be.service.ObjectStoragePresignService;
import java.net.URL;
import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PresignServiceTest {

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private AwsAppProps awsAppProps;

    @Mock
    private AwsAppProps.S3Props s3Props;

    @Mock
    private AwsAppProps.CdnProps cdnProps;

    @Mock
    private AssetPolicyProps assetPolicyProps;

    @Mock
    private AssetPolicyProps.Category imageCategory;

    @Mock
    private AssetPolicyProps.Category videoCategory;

    @Mock
    private AssetPolicyProps.Category fileCategory;

    @Mock
    private PresignRequestDto presignRequestDto;

    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;

    @InjectMocks
    private ObjectStoragePresignService presignService;

    @BeforeEach
    void setUp() {
        when(awsAppProps.getS3()).thenReturn(s3Props);
        when(awsAppProps.getCdn()).thenReturn(cdnProps);
    }

    // ===== 공통 S3/CDN 설정 세팅용 헬퍼 =====
    private void stubCommonS3AndCdn(int ttlSeconds, String cdnBaseUrl) {
        when(s3Props.getBucket()).thenReturn("test-bucket");
        when(s3Props.getBasePrefix()).thenReturn("statics");
        when(s3Props.getEnvPrefix()).thenReturn("dev");
        when(s3Props.getTtlSeconds()).thenReturn(ttlSeconds);

        when(cdnProps.getBaseUrl()).thenReturn(cdnBaseUrl);
    }

    private void stubPresignerUrl(String url) throws Exception {
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(presignedPutObjectRequest);
        when(presignedPutObjectRequest.url()).thenReturn(new URL(url));
    }

    // =========================================================
    // 1. 정상 케이스
    // =========================================================

    @Test
    @DisplayName("image/jpeg 요청 → 정상 presign 응답 & key/presignedUrl/publicUrl 검증")
    void presign_withValidImageRequest_returnsPresignResponse() throws Exception {
        // given
        when(presignRequestDto.getFileType()).thenReturn("image/jpeg");
        when(presignRequestDto.getFileName()).thenReturn("profile.jpg");

        when(assetPolicyProps.getCategories()).thenReturn(List.of(imageCategory));
        when(imageCategory.getType()).thenReturn("image");
        when(imageCategory.getAllowedContentTypes()).thenReturn(List.of("image/jpeg"));

        stubCommonS3AndCdn(600, "https://cdn.example.com");
        stubPresignerUrl("https://s3-presigned-url");

        YearMonth ym = YearMonth.now(ZoneId.of("Asia/Seoul"));
        String expectedPrefix = String.format(
                "statics/image/dev/%04d/%02d/",
                ym.getYear(), ym.getMonthValue()
        );

        // when
        PresignResponseDto response = presignService.presign(presignRequestDto);

        // then
        assertNotNull(response, "응답이 null 이면 안 된다.");
        String key = response.getObjectKey();
        assertTrue(key.startsWith(expectedPrefix), "key prefix가 예상과 다름: " + key);
        assertTrue(key.endsWith(".jpg"), "확장자는 .jpg 여야 한다: " + key);

        assertEquals("https://s3-presigned-url", response.getUploadUrl());
        assertEquals("https://cdn.example.com/" + key, response.getFileUrl());
    }

    @Test
    @DisplayName("video/mp4 요청 → video 카테고리 사용 및 예외 없이 presign")
    void presign_withValidVideoRequest_usesVideoCategory() throws Exception {
        // given
        when(presignRequestDto.getFileType()).thenReturn("video/mp4");
        when(presignRequestDto.getFileName()).thenReturn("intro.mp4");

        when(assetPolicyProps.getCategories()).thenReturn(List.of(videoCategory));
        when(videoCategory.getType()).thenReturn("video");
        when(videoCategory.getAllowedContentTypes()).thenReturn(List.of("video/mp4"));

        stubCommonS3AndCdn(600, "https://cdn.example.com");
        stubPresignerUrl("https://s3-presigned-url");

        // when
        PresignResponseDto response = presignService.presign(presignRequestDto);

        // then
        assertNotNull(response);
        assertTrue(response.getObjectKey().contains("/video/"),
                "video 카테고리이면 key에 /video/ 세그먼트가 포함되어야 한다.");
    }

    @Test
    @DisplayName("application/pdf 요청 → file 카테고리로 매핑")
    void presign_withGenericFileType_mapsToFileCategory() throws Exception {
        // given
        when(presignRequestDto.getFileType()).thenReturn("application/pdf");
        when(presignRequestDto.getFileName()).thenReturn("doc.pdf");

        when(assetPolicyProps.getCategories()).thenReturn(List.of(fileCategory));
        when(fileCategory.getType()).thenReturn("file");
        when(fileCategory.getAllowedContentTypes()).thenReturn(List.of("application/pdf"));

        stubCommonS3AndCdn(600, "https://cdn.example.com");
        stubPresignerUrl("https://s3-presigned-url");

        // when
        PresignResponseDto response = presignService.presign(presignRequestDto);

        // then
        assertNotNull(response);
        assertTrue(response.getObjectKey().contains("/file/"),
                "기타 타입은 file 카테고리로 매핑되어야 한다.");
    }

    // =========================================================
    // 2. 예외 케이스
    // =========================================================

    @Test
    @DisplayName("허용되지 않은 MIME 타입 → INVALID_FILE_TYPE")
    void presign_withUnsupportedContentType_throwsInvalidFileType() {
        // given
        when(presignRequestDto.getFileType()).thenReturn("image/webp");
        when(presignRequestDto.getFileName()).thenReturn("profile.webp");

        when(assetPolicyProps.getCategories()).thenReturn(List.of(imageCategory));
        when(imageCategory.getType()).thenReturn("image");
        when(imageCategory.getAllowedContentTypes())
                .thenReturn(List.of("image/jpeg", "image/png"));

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> presignService.presign(presignRequestDto));

        // then
        assertEquals(ObjectStorageErrorCode.INVALID_FILE_TYPE, ex.getErrorCode());
    }

    @Test
    @DisplayName("major type이 image/video가 아니고 file 카테고리도 없음 → INVALID_FILE_TYPE")
    void presign_withUnknownMajorTypeAndNoFileCategory_throwsInvalidFileType() {
        // given
        when(presignRequestDto.getFileType()).thenReturn("text/plain");
        when(presignRequestDto.getFileName()).thenReturn("note.txt");

        // image, video 카테고리만 있고 file 타입 없음
        when(assetPolicyProps.getCategories()).thenReturn(List.of(imageCategory, videoCategory));
        when(imageCategory.getType()).thenReturn("image");
        when(imageCategory.getAllowedContentTypes()).thenReturn(List.of("image/jpeg"));
        when(videoCategory.getType()).thenReturn("video");
        when(videoCategory.getAllowedContentTypes()).thenReturn(List.of("video/mp4"));

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> presignService.presign(presignRequestDto));

        // then
        assertEquals(ObjectStorageErrorCode.INVALID_FILE_TYPE, ex.getErrorCode());
    }

    @Test
    @DisplayName("카테고리 목록이 비어있는 경우 → INVALID_FILE_TYPE")
    void presign_whenCategoriesListEmpty_throwsInvalidFileType() {
        // given
        when(presignRequestDto.getFileType()).thenReturn("image/png");
        when(presignRequestDto.getFileName()).thenReturn("pic.png");

        when(assetPolicyProps.getCategories()).thenReturn(List.of()); // 빈 리스트

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> presignService.presign(presignRequestDto));

        // then
        assertEquals(ObjectStorageErrorCode.INVALID_FILE_TYPE, ex.getErrorCode());
    }

    // =========================================================
    // 3. 경계 케이스
    // =========================================================

    @Test
    @DisplayName("확장자가 없는 파일 이름 → .bin 확장자를 기본 사용")
    void presign_withFileNameWithoutExtension_usesBinAsDefaultExt() throws Exception {
        // given
        when(presignRequestDto.getFileType()).thenReturn("image/png");
        when(presignRequestDto.getFileName()).thenReturn("profile"); // 확장자 없음

        when(assetPolicyProps.getCategories()).thenReturn(List.of(imageCategory));
        when(imageCategory.getType()).thenReturn("image");
        when(imageCategory.getAllowedContentTypes()).thenReturn(List.of("image/png"));

        stubCommonS3AndCdn(600, "https://cdn.example.com");
        stubPresignerUrl("https://s3-presigned-url");

        // when
        PresignResponseDto response = presignService.presign(presignRequestDto);

        // then
        assertTrue(response.getObjectKey().toLowerCase(Locale.ROOT).endsWith(".bin"),
                "확장자가 없으면 .bin 으로 끝나야 한다: " + response.getObjectKey());
    }

    @Test
    @DisplayName("대문자 확장자 → key 내 확장자는 소문자로 변환")
    void presign_withUppercaseExtension_lowercasesExtInKey() throws Exception {
        // given
        when(presignRequestDto.getFileType()).thenReturn("image/jpeg");
        when(presignRequestDto.getFileName()).thenReturn("PROFILE.JPG");

        when(assetPolicyProps.getCategories()).thenReturn(List.of(imageCategory));
        when(imageCategory.getType()).thenReturn("image");
        when(imageCategory.getAllowedContentTypes()).thenReturn(List.of("image/jpeg"));

        stubCommonS3AndCdn(600, "https://cdn.example.com");
        stubPresignerUrl("https://s3-presigned-url");

        // when
        PresignResponseDto response = presignService.presign(presignRequestDto);

        // then
        assertTrue(response.getObjectKey().endsWith(".jpg"),
                "확장자는 소문자 .jpg 로 변환되어야 한다: " + response.getObjectKey());
    }

    @Test
    @DisplayName("CDN baseUrl 이 null/blank → S3 URL 형식으로 fallback")
    void presign_withBlankCdnBaseUrl_fallsBackToS3Url() throws Exception {
        // given
        when(presignRequestDto.getFileType()).thenReturn("image/jpeg");
        when(presignRequestDto.getFileName()).thenReturn("profile.jpg");

        when(assetPolicyProps.getCategories()).thenReturn(List.of(imageCategory));
        when(imageCategory.getType()).thenReturn("image");
        when(imageCategory.getAllowedContentTypes()).thenReturn(List.of("image/jpeg"));

        stubCommonS3AndCdn(600, "   "); // blank
        when(s3Props.getRegion()).thenReturn("ap-northeast-2");
        stubPresignerUrl("https://s3-presigned-url");

        // when
        PresignResponseDto response = presignService.presign(presignRequestDto);

        // then
        String expectedPrefix = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/";
        assertTrue(response.getFileUrl().startsWith(expectedPrefix),
                "CDN이 비어있으면 publicUrl은 S3 URL 로 fallback 해야 한다.");
    }

    @Test
    @DisplayName("CDN baseUrl 에 슬래시가 없으면 1개만 붙여서 합쳐진다")
    void presign_withCdnBaseUrlWithoutTrailingSlash_concatsWithSingleSlash() throws Exception {
        // given
        when(presignRequestDto.getFileType()).thenReturn("image/jpeg");
        when(presignRequestDto.getFileName()).thenReturn("profile.jpg");

        when(assetPolicyProps.getCategories()).thenReturn(List.of(imageCategory));
        when(imageCategory.getType()).thenReturn("image");
        when(imageCategory.getAllowedContentTypes()).thenReturn(List.of("image/jpeg"));

        stubCommonS3AndCdn(600, "https://cdn.example.com"); // 끝에 슬래시 없음
        stubPresignerUrl("https://s3-presigned-url");

        // when
        PresignResponseDto response = presignService.presign(presignRequestDto);

        // then
        assertEquals("https://cdn.example.com/" + response.getObjectKey(), response.getFileUrl());
    }

    @Test
    @DisplayName("ttlSeconds 설정값이 presign signatureDuration 에 반영된다")
    void presign_usesConfiguredTtlSecondsForSignatureDuration() throws Exception {
        // given
        int ttlSeconds = 900;

        when(presignRequestDto.getFileType()).thenReturn("image/jpeg");
        when(presignRequestDto.getFileName()).thenReturn("profile.jpg");

        when(assetPolicyProps.getCategories()).thenReturn(List.of(imageCategory));
        when(imageCategory.getType()).thenReturn("image");
        when(imageCategory.getAllowedContentTypes()).thenReturn(List.of("image/jpeg"));

        stubCommonS3AndCdn(ttlSeconds, "https://cdn.example.com");
        stubPresignerUrl("https://s3-presigned-url");

        ArgumentCaptor<PutObjectPresignRequest> captor =
                ArgumentCaptor.forClass(PutObjectPresignRequest.class);

        // when
        presignService.presign(presignRequestDto);

        // then
        verify(s3Presigner).presignPutObject(captor.capture());
        PutObjectPresignRequest captured = captor.getValue();
        assertEquals(Duration.ofSeconds(ttlSeconds), captured.signatureDuration(),
                "signatureDuration 이 ttlSeconds 설정과 일치해야 한다.");
    }
}
