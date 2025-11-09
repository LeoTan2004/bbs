package edu.xtu.bbs.common.response;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Global Response Wrapper
 * Automatically wraps Controller return values into unified ApiResponse format
 */
@RestControllerAdvice(basePackages = "edu.xtu.bbs")
public class ResponseWrapper implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(@NonNull MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {

        final Class<?> type = returnType.getParameterType();
        return type != ApiResponse.class && type != ResponseEntity.class && type != String.class;
    }

    @Override
    public Object beforeBodyWrite(@Nullable Object body,
                                  @NonNull MethodParameter returnType,
                                  @NonNull MediaType selectedContentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @NonNull ServerHttpRequest request,
                                  @NonNull ServerHttpResponse response) {

        // If body is already an ApiResponse, return directly
        if (body instanceof ApiResponse) {
            return body;
        }

        // Wrap as success response
        return ApiResponse.success(body);
    }
}