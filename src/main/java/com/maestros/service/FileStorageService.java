package com.maestros.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.maestros.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    @Autowired(required = false)
    private BlobContainerClient containerClient;

    @Value("${app.azure.storage-base-url:}")
    private String storageBaseUrl;

    public String uploadImage(MultipartFile file, String folder) throws IOException {
        if (containerClient == null) {
            throw new BadRequestException("El almacenamiento de archivos no está configurado");
        }
        // 1. Null / empty check
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo no puede estar vacío");
        }

        // 2. Content-Type whitelist
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.containsKey(contentType)) {
            throw new BadRequestException("Solo se permiten imágenes JPEG, PNG o WebP");
        }

        // 3. Size limit
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("La imagen no puede superar los 5 MB");
        }

        // 4. Magic bytes verification
        validateMagicBytes(file, contentType);

        // 5. Generate unique blob name
        String extension = ALLOWED_TYPES.get(contentType);
        String blobName = folder + "/" + UUID.randomUUID() + "-" + System.currentTimeMillis() + "." + extension;

        // 6. Get blob reference
        BlobClient blobClient = containerClient.getBlobClient(blobName);

        // 7. Upload with Content-Type header
        try (InputStream inputStream = file.getInputStream()) {
            blobClient.upload(inputStream, file.getSize(), true);
        }
        blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));

        // 8. Return public URL
        return storageBaseUrl + "/" + containerClient.getBlobContainerName() + "/" + blobName;
    }

    // -------------------------------------------------------------------------
    // Magic bytes validation
    // -------------------------------------------------------------------------

    private void validateMagicBytes(MultipartFile file, String contentType) throws IOException {
        byte[] header = new byte[12];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(header);
            if (read < 3) {
                throw new BadRequestException("El archivo no es una imagen válida");
            }
        }

        boolean valid = switch (contentType) {
            case "image/jpeg" -> isJpeg(header);
            case "image/png" -> isPng(header);
            case "image/webp" -> isWebp(header);
            default -> false;
        };

        if (!valid) {
            throw new BadRequestException("El archivo no es una imagen válida");
        }
    }

    // JPEG: FF D8 FF
    private boolean isJpeg(byte[] h) {
        return (h[0] & 0xFF) == 0xFF
                && (h[1] & 0xFF) == 0xD8
                && (h[2] & 0xFF) == 0xFF;
    }

    // PNG: 89 50 4E 47
    private boolean isPng(byte[] h) {
        return h.length >= 4
                && (h[0] & 0xFF) == 0x89
                && (h[1] & 0xFF) == 0x50
                && (h[2] & 0xFF) == 0x4E
                && (h[3] & 0xFF) == 0x47;
    }

    // WebP: "RIFF" at 0-3 and "WEBP" at 8-11
    private boolean isWebp(byte[] h) {
        if (h.length < 12)
            return false;
        return h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
    }
}
