package com.example.kvm.backend.service;

import com.example.kvm.common.dto.ImageInfoDto;
import com.example.kvm.common.request.AddImageRequest;
import java.util.List;

public interface ImageService {
    List<ImageInfoDto> listImages();
    ImageInfoDto addImage(AddImageRequest request);
    void deleteImage(String name);
}
