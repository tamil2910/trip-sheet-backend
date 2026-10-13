package com.example.trip_sheet_backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.MediaUploadRequest;
import com.example.trip_sheet_backend.dtos.MediaUploadResponse;
import com.example.trip_sheet_backend.services.RailwayBucketStorageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/media")
public class MediaController {
  private final RailwayBucketStorageService storage;

  public MediaController(RailwayBucketStorageService storage) {
    this.storage = storage;
  }

  @PostMapping("/upload")
  public ResponseEntity<MediaUploadResponse> upload(@Valid @RequestBody MediaUploadRequest request) {
    String key = storage.storeBase64(request.category(), request.base64());
    return ResponseEntity.ok(new MediaUploadResponse(key, storage.createDownloadUrl(key)));
  }

  @GetMapping("/download-url")
  public ResponseEntity<MediaUploadResponse> downloadUrl(@RequestParam String key) {
    return ResponseEntity.ok(new MediaUploadResponse(key, storage.createDownloadUrl(key)));
  }
}
