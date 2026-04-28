package com.tiv.auto.code.review.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tiv.auto.code.review.constants.AutoCodeReviewConstants;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class LLMClientClient implements LLMClient {

    protected final String model;

    protected final String apiKey;

    protected final String baseUrl;

    protected final String provider;

    protected final ObjectMapper objectMapper;

    protected final OkHttpClient okHttpClient;

    public LLMClientClient(String model, String apiKey, String baseUrl, String provider) {
        this.model = model;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.provider = provider;
        this.objectMapper = new ObjectMapper();
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .hostnameVerifier((hostname, session) -> true)
                .build();
    }

    @Override
    public String chat(List<Map<String, String>> msgs) {
        try {


            // 1. 构建请求体
            ObjectNode body = objectMapper.createObjectNode();
            body.put(AutoCodeReviewConstants.MODEL, model);

            ArrayNode messages = objectMapper.createArrayNode();
            for (Map<String, String> msg : msgs) {
                ObjectNode message = objectMapper.createObjectNode();
                message.put(AutoCodeReviewConstants.ROLE, msg.get(AutoCodeReviewConstants.ROLE));
                message.put(AutoCodeReviewConstants.CONTENT, msg.get(AutoCodeReviewConstants.CONTENT));
                messages.add(message);
            }

            body.set(AutoCodeReviewConstants.MESSAGES, messages);
            body.put(AutoCodeReviewConstants.TEMPERATURE, 0.1);

            // 2. 构建 url
            String url = baseUrl + AutoCodeReviewConstants.CHAT_COMPLETIONS_PATH;

            // 3. 创建 http 请求
            RequestBody requestBody = RequestBody.create(
                    objectMapper.writeValueAsString(body),
                    MediaType.parse(AutoCodeReviewConstants.MEDIA_TYPE_JSON)
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader(AutoCodeReviewConstants.AUTHORIZATION, AutoCodeReviewConstants.BEARER + " " + apiKey)
                    .addHeader(AutoCodeReviewConstants.CONTENT_TYPE, AutoCodeReviewConstants.APPLICATION_JSON)
                    .post(requestBody)
                    .build();

            log.info("开始调用 {} 模型, request: {}", model, request);

            // 4. 解析 http 响应
            try (Response response = okHttpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    log.error("调用 {} 模型失败, responseBody: {}", model, responseBody);
                    throw new RuntimeException("调用 " + model + " 模型失败");
                }
                JsonNode responseJson = objectMapper.readTree(responseBody);
                String content = responseJson.path(AutoCodeReviewConstants.CHOICES)
                        .path(0)
                        .path(AutoCodeReviewConstants.MESSAGE)
                        .path(AutoCodeReviewConstants.CONTENT)
                        .asText();
                log.info("调用 {} 模型成功, content: {}", model, content);
                return content;
            }
        } catch (IOException e) {
            log.error("调用 {} 模型失败, IO异常", model, e);
            throw new RuntimeException("调用 " + model + " 模型失败 " + e.getMessage());
        }
    }

}