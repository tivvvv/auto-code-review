package com.tiv.auto.code.review.llm.minimax;

import com.tiv.auto.code.review.llm.AbstractLLMClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MiniMaxLLMClient extends AbstractLLMClient {

    public MiniMaxLLMClient(String model, String apiKey, String baseUrl, String provider) {
        super(model, apiKey, baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl, provider);
        log.info("MiniMax 客户端初始化成功, model: {}, baseUrl: {}", model, baseUrl);
    }

}