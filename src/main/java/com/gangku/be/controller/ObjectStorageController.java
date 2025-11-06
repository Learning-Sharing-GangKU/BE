package com.gangku.be.controller;

import com.gangku.be.dto.object.PresignRequestDto;
import com.gangku.be.dto.object.PresignResponseDto;
import com.gangku.be.service.ObjectStoragePresignService;
import com.gangku.be.service.ObjectStoragePresignService.BadRequestApiException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/objects")
@RequiredArgsConstructor
public class ObjectStorageController {

    private final ObjectStoragePresignService service;

    @PostMapping("/presigned-url")
    public PresignResponseDto presign(@RequestBody @Valid PresignRequestDto req) {
        try {
            ObjectStoragePresignService.Result r = service.presign(req);
            return new PresignResponseDto(r.objectKey(), r.uploadUrl(), r.fileUrl(), r.expiresIn());
        } catch (BadRequestApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ServerErrorApiException("PRESIGNED_URL_GENERATION_FAILED", "Presigned URL 생성 중 오류가 발생했습니다.");
        }
    }
}
