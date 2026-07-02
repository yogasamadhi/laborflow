package com.company.labor.module.file.storage;

import com.company.labor.common.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "laborflow.file.storage", name = "type", havingValue = "object")
public class HttpObjectFileStorageService implements FileStorageService {
    private final ObjectStorageProperties properties;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public void store(MultipartFile file, String fileName) throws IOException {
        ensureConfigured();
        HttpRequest.Builder builder = HttpRequest.newBuilder(objectUri(fileName))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()));
        if (StringUtils.hasText(file.getContentType())) {
            builder.header("Content-Type", file.getContentType());
        }
        applyAuth(builder);
        HttpResponse<Void> response = send(builder.build(), HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException("对象存储写入失败");
        }
    }

    @Override
    public Resource load(String fileName) throws IOException {
        ensureConfigured();
        HttpRequest.Builder builder = HttpRequest.newBuilder(objectUri(fileName)).GET();
        applyAuth(builder);
        HttpResponse<InputStream> response = send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() == 404) {
            throw new BusinessException("文件不存在或已被移除");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException("对象存储读取失败");
        }
        return new InputStreamResource(response.body());
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(properties.getEndpoint()) || !StringUtils.hasText(properties.getBucket())) {
            throw new IllegalStateException("Object storage endpoint and bucket must be configured");
        }
    }

    private void applyAuth(HttpRequest.Builder builder) {
        if (StringUtils.hasText(properties.getAuthHeaderName()) && StringUtils.hasText(properties.getAuthHeaderValue())) {
            builder.header(properties.getAuthHeaderName(), properties.getAuthHeaderValue());
        }
    }

    private URI objectUri(String fileName) {
        String endpoint = properties.getEndpoint().replaceAll("/+$", "");
        return URI.create(endpoint + "/" + encode(properties.getBucket()) + "/" + encode(fileName));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
        try {
            return httpClient.send(request, handler);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Object storage request interrupted", ex);
        }
    }
}
