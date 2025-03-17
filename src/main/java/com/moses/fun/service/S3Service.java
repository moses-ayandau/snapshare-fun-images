package com.moses.fun.service;

import com.moses.fun.model.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;

    private static final String BUCKET_NAME = "fun-social-images-s3-bucket-1";

    public S3Service() {
        s3Client = S3Client.builder()
                .region(Region.US_WEST_2)
                .credentialsProvider(DefaultCredentialsProvider.create()) // Use the default credentials provider
                .build();
    }

    public List<Image> listImages(int page, int size) {
        try {
            // Get all objects first (this could be optimized for very large buckets)
            List<S3Object> allObjects = listAllObjects();

            // Calculate start and end indices for the requested page
            int startIndex = (page - 1) * size;

            // Check if the requested page is valid
            if (startIndex >= allObjects.size()) {
                return Collections.emptyList();
            }

            // Calculate end index, ensuring we don't go beyond the list size
            int endIndex = Math.min(startIndex + size, allObjects.size());

            // Extract the sublist for the requested page
            List<S3Object> pageObjects = allObjects.subList(startIndex, endIndex);

            // Map the S3Objects to Image objects
            return pageObjects.stream()
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

    // Helper method to get all objects from the bucket
    private List<S3Object> listAllObjects() {
        List<S3Object> allObjects = new ArrayList<>();
        String nextContinuationToken = null;

        do {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(BUCKET_NAME);

            if (nextContinuationToken != null) {
                requestBuilder.continuationToken(nextContinuationToken);
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
            allObjects.addAll(response.contents());

            if (response.isTruncated()) {
                nextContinuationToken = response.nextContinuationToken();
            } else {
                nextContinuationToken = null;
            }
        } while (nextContinuationToken != null);

        return allObjects;
    }

    public int countImages() {
        try {
            // This could be optimized by caching the count or using a database
            return listAllObjects().size();
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
                    .bucket(BUCKET_NAME)
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
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            log.error("Error deleting image from S3", e);
            throw new RuntimeException("Failed to delete image", e);
        }
    }

    private String generatePresignedUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", BUCKET_NAME, Region.US_WEST_2, key);
    }
}