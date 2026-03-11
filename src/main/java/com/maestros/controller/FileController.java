package com.maestros.controller;

import com.maestros.dto.response.ApiResponse;
import com.maestros.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private static final Set<String> ALLOWED_FOLDERS = Set.of("profile-photos", "work-photos");

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "uploads") String folder) throws IOException {

        // Folder whitelist — fall back to "uploads" for unknown values
        String sanitizedFolder = ALLOWED_FOLDERS.contains(folder) ? folder : "uploads";

        String publicUrl = fileStorageService.uploadImage(file, sanitizedFolder);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("url", publicUrl), "Imagen subida exitosamente"));
    }
}
