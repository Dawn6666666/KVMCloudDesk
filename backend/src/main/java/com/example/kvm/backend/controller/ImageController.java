package com.example.kvm.backend.controller;

import com.example.kvm.backend.service.ImageService;
import com.example.kvm.common.dto.ImageInfoDto;
import com.example.kvm.common.request.AddImageRequest;
import com.example.kvm.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/images")
public class ImageController {
    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping
    public ApiResponse<List<ImageInfoDto>> list() {
        return ApiResponse.ok(imageService.listImages());
    }

    @PostMapping
    public ApiResponse<ImageInfoDto> add(@RequestBody AddImageRequest request) {
        return ApiResponse.ok("镜像添加成功", imageService.addImage(request));
    }

    @DeleteMapping("/{name}")
    public ApiResponse<Void> delete(@PathVariable("name") String name) {
        imageService.deleteImage(name);
        return ApiResponse.ok("镜像删除成功", null);
    }
}
