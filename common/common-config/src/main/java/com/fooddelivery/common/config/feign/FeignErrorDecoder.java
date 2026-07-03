package com.fooddelivery.common.config.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.common.lib.dto.ApiResponse;
import com.fooddelivery.common.lib.exception.ApiException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        try (InputStream body = response.body().asInputStream()) {
            ApiResponse<?> apiResponse = objectMapper.readValue(body, ApiResponse.class);
            String message = apiResponse.getMessage() != null ? apiResponse.getMessage() : "Unknown error";
            return new ApiException(message, HttpStatus.valueOf(response.status()));
        } catch (IOException e) {
            log.warn("Could not parse Feign error response body for {}", methodKey);
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
