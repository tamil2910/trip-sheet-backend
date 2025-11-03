package com.example.trip_sheet_backend.response_setups;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  @Nullable
  public Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType, MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        // 🔹 Avoid double-wrapping if it's already an ApiResponse or ResponseEntity
        if (body instanceof ApiResponse || body instanceof ResponseEntity) {
            return body;
        }

        if(body == null) return new ApiResponse<>(true, "No Content", null);

        return new ApiResponse<>(true, "Success", body);
  }



}
