package com.moses.fun.service;

import com.moses.fun.model.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    public List<Image> listImages(int page, int size) {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .maxKeys(size)
                    .continuationToken(page > 1 ? String.valueOf((page - 1) * size) : null)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            return response.contents().stream()
                    .map(s3Object -> Image.builder()
                            .key(s3Object.key())
                            .fileName(s3Object.key())
                            .url(generatePresignedUrl(s3Object.key()))
                            .size(s3Object.size())
                            .lastModified(s3Object.lastModified())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error listing images from S3", e);
            throw new RuntimeException("Failed to list images", e);
        }
    }

    public int countImages() {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            return response.keyCount();
        } catch (Exception e) {
            log.error("Error counting images in S3", e);
            throw new RuntimeException("Failed to count images", e);
        }
    }

    public Image uploadImage(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String key = UUID.randomUUID().toString() + extension;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            return Image.builder()
                    .key(key)
                    .fileName(originalFilename)
                    .url(generatePresignedUrl(key))
                    .size(file.getSize())
                    .lastModified(Instant.now())
                    .build();
        } catch (IOException e) {
            log.error("Error uploading image to S3", e);
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    public void deleteImage(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            log.error("Error deleting image from S3", e);
            throw new RuntimeException("Failed to delete image", e);
        }
    }

    private String generatePresignedUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    }

}