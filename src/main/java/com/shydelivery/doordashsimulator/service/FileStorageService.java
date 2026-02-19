package com.shydelivery.doordashsimulator.service;

import com.shydelivery.doordashsimulator.dto.response.UploadResponse;
import com.shydelivery.doordashsimulator.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public UploadResponse storeMenuItemImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("仅支持图片格式上传");
        }

        String extension = getExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + extension;
        Path targetDir = Paths.get(uploadDir, "menu-items").toAbsolutePath().normalize();

        try {
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Menu image stored: {}", targetPath);
        } catch (IOException ex) {
            throw new BusinessException("图片保存失败，请重试", ex);
        }

        String url = "/api/uploads/menu-items/" + fileName;
    return new UploadResponse(url, fileName, contentType, file.getSize());
    }

    private String getExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "";
        }
        int index = originalFilename.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        return originalFilename.substring(index).toLowerCase();
    }
}
