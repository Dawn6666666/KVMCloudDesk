package com.example.kvm.backend.service.mock;

import com.example.kvm.backend.exception.BusinessException;
import com.example.kvm.backend.service.ImageService;
import com.example.kvm.common.dto.ImageInfoDto;
import com.example.kvm.common.request.AddImageRequest;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock")
public class MockImageService implements ImageService {
    private final MockDataStore store;

    public MockImageService(MockDataStore store) {
        this.store = store;
    }

    @Override
    public List<ImageInfoDto> listImages() {
        return store.images.values().stream().sorted(Comparator.comparing(i -> i.name)).toList();
    }

    @Override
    public ImageInfoDto addImage(AddImageRequest request) {
        if (request.name == null || request.name.isBlank()) {
            throw new BusinessException("镜像名称不能为空");
        }
        ImageInfoDto dto = new ImageInfoDto();
        dto.name = request.name;
        dto.path = request.path;
        dto.format = request.name.endsWith(".iso") ? "iso" : request.name.endsWith(".img") ? "raw" : "qcow2";
        dto.sizeGb = 1;
        dto.physicalSizeGb = 0.8;
        dto.exists = true;
        dto.createTime = MockDataStore.now();
        dto.description = request.description;
        store.images.put(dto.name, dto);
        return dto;
    }

    @Override
    public void deleteImage(String name) {
        if (store.images.remove(name) == null) {
            throw new BusinessException("镜像不存在：" + name);
        }
    }
}
