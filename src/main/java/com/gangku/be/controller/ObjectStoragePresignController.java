package com.gangku.be.controller;

import com.gangku.be.dto.object.PresignRequestDto;
import com.gangku.be.dto.object.PresignResponseDto;
import com.gangku.be.service.ObjectStoragePresignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/objects")
@RequiredArgsConstructor
public class ObjectStoragePresignController {

    private final ObjectStoragePresignService objectStoragePresignService;

    @PostMapping("/presigned-url")
    public PresignResponseDto presign(@RequestBody @Valid PresignRequestDto presignRequestDto) {
        return objectStoragePresignService.presign(presignRequestDto);
    }
}
