package com.shydelivery.doordashsimulator.controller;

import com.shydelivery.doordashsimulator.dto.response.UploadResponse;
import com.shydelivery.doordashsimulator.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/uploads")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);
    private final FileStorageService fileStorageService;

    public UploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    @PostMapping("/menu-items")
    public ResponseEntity<UploadResponse> uploadMenuItemImage(@RequestParam("file") MultipartFile file) {
        log.info("上传菜单图片: name={}, size={}", file.getOriginalFilename(), file.getSize());
        UploadResponse response = fileStorageService.storeMenuItemImage(file);
        return ResponseEntity.ok(response);
    }
}
