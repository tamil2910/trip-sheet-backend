package com.example.trip_sheet_backend.services;

import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
public class RailwayBucketStorageService {
  private static final int MAX_FILE_BYTES = 10 * 1024 * 1024;
  private final ObjectProvider<S3Client> s3Provider;
  private final ObjectProvider<S3Presigner> presignerProvider;
  private final String bucket;

  public RailwayBucketStorageService(
      ObjectProvider<S3Client> s3Provider,
      ObjectProvider<S3Presigner> presignerProvider,
      @Value("${BUCKET_NAME:}") String bucket) {
    this.s3Provider = s3Provider;
    this.presignerProvider = presignerProvider;
    this.bucket = bucket;
  }

  /** Accepts either raw Base64 or a data URI and returns the stable key to persist in the model. */
  public String storeBase64(String category, String base64) {
    if (!StringUtils.hasText(base64)) return null;
    ensureConfigured();

    String value = base64.trim();
    String contentType = "application/octet-stream";
    int comma = value.indexOf(',');
    if (value.startsWith("data:") && comma > 0) {
      String metadata = value.substring(5, comma);
      int semicolon = metadata.indexOf(';');
      contentType = (semicolon >= 0 ? metadata.substring(0, semicolon) : metadata).toLowerCase(Locale.ROOT);
      value = value.substring(comma + 1);
    }
    if (value.length() > (MAX_FILE_BYTES * 4L / 3L) + 8L) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must not exceed 10 MB");
    }

    byte[] bytes;
    try {
      bytes = decodeBase64(value);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File content must be valid Base64");
    }
    if (bytes.length == 0 || bytes.length > MAX_FILE_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be between 1 byte and 10 MB");
    }
    String detectedType = detectType(bytes);
    if (contentType.equals("application/octet-stream")) contentType = detectedType;
    else if (!contentType.equals(detectedType)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File content does not match its declared type");
    }
    if (!isSupportedType(contentType)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, WebP and PDF files are supported");
    }

    String extension = extensionFor(contentType);
    String key = safeCategory(category) + "/" + UUID.randomUUID() + extension;
    try {
      s3Provider.getObject().putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(), RequestBody.fromBytes(bytes));
      return key;
    } catch (S3Exception exception) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not save the file to Railway Bucket", exception);
    }
  }

  /** Stores a validated PDF or image upload and returns the private bucket key. */
  public String storeFile(String category, byte[] bytes, String declaredContentType) {
    ensureConfigured();
    if (bytes == null || bytes.length == 0 || bytes.length > MAX_FILE_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be between 1 byte and 10 MB");
    }
    String detectedType = detectType(bytes);
    String contentType = declaredContentType == null ? "" : declaredContentType.toLowerCase(Locale.ROOT);
    if (!isSupportedType(detectedType) || (!contentType.isBlank() && !contentType.equals("application/octet-stream")
        && !contentType.equals(detectedType))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only valid JPEG, PNG, WebP, GIF, BMP and PDF files are supported");
    }
    String key = safeCategory(category) + "/" + UUID.randomUUID() + extensionFor(detectedType);
    try {
      s3Provider.getObject().putObject(
          PutObjectRequest.builder().bucket(bucket).key(key).contentType(detectedType).build(),
          RequestBody.fromBytes(bytes));
      return key;
    } catch (S3Exception exception) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not save the file to Railway Bucket", exception);
    }
  }

  /** URL is temporary because Railway Buckets are private; persist the returned key, not this URL. */
  public String createDownloadUrl(String key) {
    ensureConfigured();
    try {
      GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(key).build();
      return presignerProvider.getObject().presignGetObject(GetObjectPresignRequest.builder()
          .signatureDuration(Duration.ofHours(1)).getObjectRequest(get).build()).url().toString();
    } catch (S3Exception exception) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not create a Railway Bucket download link", exception);
    }
  }

  public boolean isBase64(String value) {
    if (value == null) return false;
    String trimmed = value.trim();
    if (trimmed.startsWith("data:")) return true;
    try {
      return decodeBase64(trimmed).length > 0;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  public String storeIfBase64(String category, String value) {
    return isBase64(value) ? storeBase64(category, value) : value;
  }

  private void ensureConfigured() {
    if (s3Provider.getIfAvailable() == null || presignerProvider.getIfAvailable() == null || !StringUtils.hasText(bucket)) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Railway Bucket is not configured; set BUCKET_ENDPOINT, BUCKET_ACCESS_KEY_ID, BUCKET_SECRET_ACCESS_KEY and BUCKET_NAME");
    }
  }

  private static String safeCategory(String category) {
    String normalized = category == null ? "uploads" : category.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
    return normalized.isBlank() ? "uploads" : normalized;
  }

  private static boolean isSupportedType(String type) {
    return type.equals("image/jpeg") || type.equals("image/png") || type.equals("image/webp")
        || type.equals("image/gif") || type.equals("image/bmp") || type.equals("application/pdf");
  }

  private static String extensionFor(String type) {
    return switch (type) {
      case "image/jpeg" -> ".jpg";
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      case "image/gif" -> ".gif";
      case "image/bmp" -> ".bmp";
      case "application/pdf" -> ".pdf";
      default -> ".bin";
    };
  }

  private static byte[] decodeBase64(String value) {
    try {
      return Base64.getDecoder().decode(value);
    } catch (IllegalArgumentException ignored) {
      try {
        return Base64.getUrlDecoder().decode(value);
      } catch (IllegalArgumentException ignoredUrlSafe) {
        return Base64.getMimeDecoder().decode(value);
      }
    }
  }

  private static String detectType(byte[] bytes) {
    if (bytes.length >= 4 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) return "image/jpeg";
    if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return "image/png";
    if (bytes.length >= 12 && bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46
        && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50) return "image/webp";
    if (bytes.length >= 6 && bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46
        && bytes[3] == 0x38 && (bytes[4] == 0x37 || bytes[4] == 0x39) && bytes[5] == 0x61) return "image/gif";
    if (bytes.length >= 2 && bytes[0] == 0x42 && bytes[1] == 0x4D) return "image/bmp";
    if (bytes.length >= 5 && bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46 && bytes[4] == 0x2D) return "application/pdf";
    return "application/octet-stream";
  }
}
