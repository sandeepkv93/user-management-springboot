package com.example.usermanagement.service;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
@RequiredArgsConstructor
public class S3Service {
  private final S3Client s3Client;

  @Value("${app.aws.s3.bucket-name}")
  private String bucketName;

  public String uploadFile(MultipartFile file, String directory) {
    try {
      String fileExtension = getFileExtension(Objects.requireNonNull(file.getOriginalFilename()));
      String key = directory + "/" + UUID.randomUUID() + fileExtension;

      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder()
              .bucket(bucketName)
              .key(key)
              .contentType(file.getContentType())
              .build();

      s3Client.putObject(
          putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

      return key;
    } catch (IOException e) {
      throw new RuntimeException("Failed to upload file", e);
    }
  }

  public void deleteFile(String fileUrl) {
    if (!StringUtils.hasText(fileUrl)) {
      return;
    }

    DeleteObjectRequest deleteObjectRequest =
        DeleteObjectRequest.builder().bucket(bucketName).key(fileUrl).build();

    s3Client.deleteObject(deleteObjectRequest);
  }

  private String getFileExtension(String filename) {
    return filename.substring(filename.lastIndexOf("."));
  }
}
