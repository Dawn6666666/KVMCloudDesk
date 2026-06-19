package com.example.kvm.backend.service.libvirt;

import com.example.kvm.backend.exception.BusinessException;
import com.example.kvm.backend.service.ImageService;
import com.example.kvm.common.dto.ImageInfoDto;
import com.example.kvm.common.request.AddImageRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("libvirt")
public class LibvirtResourceFallbackServices implements ImageService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private final Path imageDir;

    public LibvirtResourceFallbackServices(@Value("${kvm.image.dir}") String imageDir) {
        this.imageDir = Path.of(imageDir);
    }

    @Override
    public List<ImageInfoDto> listImages() {
        if (!Files.isDirectory(imageDir)) {
            return List.of();
        }
        try (var stream = Files.list(imageDir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(this::isImage)
                    .map(this::image)
                    .toList();
        } catch (IOException ex) {
            throw new BusinessException("扫描镜像目录失败：" + ex.getMessage());
        }
    }

    @Override
    public ImageInfoDto addImage(AddImageRequest request) {
        throw unsupported("libvirt 模式暂未实现添加镜像，请先将镜像放入 " + imageDir);
    }

    @Override
    public void deleteImage(String name) {
        throw unsupported("libvirt 模式暂未实现删除镜像");
    }

    private boolean isImage(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".img") || name.endsWith(".qcow2") || name.endsWith(".iso");
    }

    private ImageInfoDto image(Path path) {
        try {
            ImageInfoDto dto = new ImageInfoDto();
            dto.name = path.getFileName().toString();
            dto.path = path.toString();
            dto.format = format(dto.name);
            dto.sizeGb = Math.round(Files.size(path) / 1024.0 / 1024.0 / 1024.0 * 10.0) / 10.0;
            dto.createTime = FORMATTER.format(Instant.ofEpochMilli(Files.getLastModifiedTime(path).toMillis()));
            dto.description = "libvirt 镜像文件";
            return dto;
        } catch (IOException ex) {
            throw new BusinessException("读取镜像信息失败：" + path);
        }
    }

    private String format(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".iso")) {
            return "iso";
        }
        if (lower.endsWith(".img")) {
            return "raw";
        }
        return "qcow2";
    }

    private BusinessException unsupported(String message) {
        return new BusinessException(message);
    }
}
