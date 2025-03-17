package by.baes.photos3service.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoService {
    private final AmazonS3 amazonS3;

    @Value("${s3.bucket:photos}")
    private String bucket;

    @PostConstruct
    public void init() {
        try {
            if (!amazonS3.doesBucketExistV2(bucket)) {
                amazonS3.createBucket(bucket);
                log.info("Bucket {} created successfully", bucket);
            } else {
                log.info("Bucket {} already exists", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to initialize bucket {}: {}", bucket, e.getMessage(), e);
            throw new RuntimeException("Failed to initialize S3 bucket", e);
        }
    }

    public String uploadPhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Upload attempt with empty or null file");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File cannot be empty");
        }

        try {
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            amazonS3.putObject(bucket, fileName, file.getInputStream(), metadata);
            log.info("Photo uploaded successfully: {}", fileName);
            return fileName;
        } catch (IOException e) {
            log.error("Failed to upload photo: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload photo", e);
        } catch (Exception e) {
            log.error("Unexpected error during photo upload: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error during upload", e);
        }
    }

    public byte[] getPhoto(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            log.warn("Get photo attempt with empty or null fileName");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name cannot be empty");
        }

        try {
            S3Object s3Object = amazonS3.getObject(bucket, fileName);
            try (var inputStream = s3Object.getObjectContent()) {
                byte[] content = inputStream.readAllBytes();
                log.info("Photo retrieved successfully: {}", fileName);
                return content;
            }
        } catch (IOException e) {
            log.error("Failed to read photo {}: {}", fileName, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read photo", e);
        } catch (Exception e) {
            log.error("Photo {} not found or inaccessible: {}", fileName, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found", e);
        }
    }

    public void deletePhoto(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            log.warn("Delete photo attempt with empty or null fileName");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name cannot be empty");
        }

        try {
            amazonS3.deleteObject(bucket, fileName);
            log.info("Photo deleted successfully: {}", fileName);
        } catch (Exception e) {
            log.error("Failed to delete photo {}: {}", fileName, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete photo", e);
        }
    }
}