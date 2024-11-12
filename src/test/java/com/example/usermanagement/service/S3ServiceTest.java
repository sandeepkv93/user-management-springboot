package com.example.usermanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

  @Mock private S3Client s3Client;
  @InjectMocks private S3Service s3Service;

  private MultipartFile testFile;
  private static final String BUCKET_NAME = "user-profiles";

  @BeforeEach
  void setUp() {
    testFile =
        new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image content".getBytes());
    ReflectionTestUtils.setField(s3Service, "bucketName", BUCKET_NAME);
  }

  @Test
  @DisplayName("Should successfully upload file")
  void shouldUploadFile() {
    // Arrange
    String directory = "profile-pictures/123";
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());

    // Act
    String result = s3Service.uploadFile(testFile, directory);

    // Assert
    assertThat(result).isNotNull().startsWith(directory).endsWith(".jpg");

    verify(s3Client)
        .putObject(
            argThat(
                (PutObjectRequest request) ->
                    request.bucket().equals(BUCKET_NAME)
                        && request.key().startsWith(directory)
                        && request.key().endsWith(".jpg")),
            any(RequestBody.class));
  }

  @Test
  @DisplayName("Should throw exception when upload fails")
  void shouldThrowExceptionWhenUploadFails() {
    // Arrange
    String directory = "profile-pictures/123";
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(new RuntimeException("Upload failed"));

    // Act & Assert
    assertThrows(RuntimeException.class, () -> s3Service.uploadFile(testFile, directory));
  }

  @Test
  @DisplayName("Should handle IO exception during upload")
  void shouldHandleIOExceptionDuringUpload() throws IOException {
    // Arrange
    String directory = "profile-pictures/123";
    MultipartFile badFile = mock(MultipartFile.class);
    when(badFile.getInputStream()).thenThrow(new IOException("Failed to read file"));
    when(badFile.getOriginalFilename()).thenReturn("test.jpg");
    when(badFile.getContentType()).thenReturn("image/jpeg");

    // Act & Assert
    assertThrows(RuntimeException.class, () -> s3Service.uploadFile(badFile, directory));
    verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  @DisplayName("Should successfully delete file")
  void shouldDeleteFile() {
    // Arrange
    String fileUrl = "profile-pictures/123/test.jpg";
    when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenReturn(DeleteObjectResponse.builder().build());

    // Act
    s3Service.deleteFile(fileUrl);

    // Assert
    verify(s3Client)
        .deleteObject(
            argThat(
                (DeleteObjectRequest request) ->
                    request.bucket().equals(BUCKET_NAME) && request.key().equals(fileUrl)));
  }

  @Test
  @DisplayName("Should handle null file url during delete")
  void shouldHandleNullFileUrlDuringDelete() {
    // Act
    s3Service.deleteFile(null);

    // Assert
    verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
  }

  @Test
  @DisplayName("Should handle empty file url during delete")
  void shouldHandleEmptyFileUrlDuringDelete() {
    // Act
    s3Service.deleteFile("");

    // Assert
    verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
  }

  @Test
  @DisplayName("Should handle whitespace file url during delete")
  void shouldHandleWhitespaceFileUrlDuringDelete() {
    // Act
    s3Service.deleteFile("   ");

    // Assert
    verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
  }

  @Test
  @DisplayName("Should handle file with no extension")
  void shouldHandleFileWithNoExtension() {
    // Arrange
    MultipartFile fileWithNoExt =
        new MockMultipartFile(
            "file", "testfile", "application/octet-stream", "test content".getBytes());

    // Act & Assert
    assertThrows(RuntimeException.class, () -> s3Service.uploadFile(fileWithNoExt, "test-dir"));
    verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }
}
