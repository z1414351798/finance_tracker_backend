package com.z.finance.tracker.service;

import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    /** Internal endpoint the MinioClient was built with, e.g. http://localhost:9000 */
    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * Public base URL reachable by clients (Android).
     * In dev: set to your ngrok URL so presigned URLs go ngrok→nginx→MinIO.
     * In prod: set to your domain, e.g. https://yourdomain.com
     * Defaults to the internal endpoint (no-op rewrite) if not set.
     */
    @Value("${minio.public-url:#{null}}")
    private String publicUrl;

    // ── Upload ────────────────────────────────────────────────────────────────
    public String upload(MultipartFile file, String objectName) throws Exception {
        boolean exists = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(bucket).build()
        );
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Created MinIO bucket: {}", bucket);
        }

        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build()
        );

        log.info("Uploaded to MinIO [object={}, size={}B, type={}]",
                objectName, file.getSize(), file.getContentType());
        return objectName;
    }

    // ── Download (proxy) ──────────────────────────────────────────────────────
    public byte[] getBytes(String stored) throws Exception {
        String objectName = toObjectName(stored);
        log.debug("Fetching from MinIO [object={}]", objectName);
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build())) {
            byte[] bytes = stream.readAllBytes();
            log.debug("Fetched {} bytes from MinIO [object={}]", bytes.length, objectName);
            return bytes;
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    public void delete(String stored) {
        if (stored == null) return;
        try {
            String objectName = toObjectName(stored);
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build()
            );
            log.info("Deleted from MinIO [object={}]", objectName);
        } catch (Exception e) {
            log.error("MinIO delete failed [stored={}, error={}]", stored, e.getMessage());
        }
    }

    // ── Content-type helper ───────────────────────────────────────────────────
    public static String contentTypeFor(String objectName) {
        if (objectName == null) return "image/jpeg";
        String lower = objectName.toLowerCase();
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif"))  return "image/gif";
        return "image/jpeg";
    }

    // ── Extract bare object name ──────────────────────────────────────────────
    private String toObjectName(String stored) {
        if (!stored.startsWith("http")) return stored;

        int bucketIdx = stored.indexOf("/" + bucket + "/");
        if (bucketIdx >= 0) return stored.substring(bucketIdx + bucket.length() + 2);

        int uploadsIdx = stored.indexOf("/uploads/");
        if (uploadsIdx >= 0) return stored.substring(uploadsIdx + 9);

        int lastSlash = stored.lastIndexOf('/');
        return lastSlash >= 0 ? stored.substring(lastSlash + 1) : stored;
    }

    /**
     * Generates a presigned GET URL valid for 60 minutes.
     *
     * The MinIO client signs with the internal endpoint (e.g. http://localhost:9000).
     * We then rewrite that to the public URL (e.g. https://xxx.ngrok-free.dev) so
     * Android can reach it via ngrok → nginx → MinIO.
     * nginx must set Host to the internal endpoint so MinIO's signature check passes.
     */
    public String generatePresignedUrl(String objectName) throws Exception {
        String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(objectName)
                        .expiry(60, TimeUnit.MINUTES)
                        .build()
        );
        // Rewrite internal endpoint → public URL so Android can reach MinIO directly
        if (publicUrl != null && !publicUrl.isBlank() && !publicUrl.equals(endpoint)) {
            url = url.replace(endpoint, publicUrl);
        }
        return url;
    }
}
