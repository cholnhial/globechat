package dev.chol.globechat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service for storing moodsic files on disk.
 */
@Service
public class MoodsicStorageService {

    private final Path storageLocation;

    public MoodsicStorageService(@Value("${globechat.moodsic.storage-path}") String storagePath) {
        this.storageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create moodsic storage directory", e);
        }
    }

    /**
     * Store a file and return the relative path.
     */
    public String store(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = UUID.randomUUID() + extension;
        Path targetLocation = this.storageLocation.resolve(filename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        }

        return filename;
    }

    /**
     * Delete a file by its relative path.
     */
    public void delete(String filePath) throws IOException {
        Path file = this.storageLocation.resolve(filePath).normalize();
        Files.deleteIfExists(file);
    }

    /**
     * Get the full path for a file.
     */
    public Path getFilePath(String filePath) {
        return this.storageLocation.resolve(filePath).normalize();
    }
}
