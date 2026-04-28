package com.tiv.auto.code.review.llm;

import com.tiv.auto.code.review.llm.minimax.MiniMaxLLMClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LLMFactory {

    @Value("${llm.provider}")
    private String defaultProvider;

    @Value("${llm.minimax.api-key}")
    private String miniMaxAPIKey;

    @Value("${llm.minimax.base-url}")
    private String miniMaxBaseUrl;

    @Value("${llm.minimax.model}")
    private String miniMaxModel;

    public LLMClient getClient() {
        return getClient(ModelProvider.getEnumByName(defaultProvider));
    }

    public LLMClient getClient(ModelProvider modelProvider) {
        if (modelProvider == null) {
            return null;
        }
        switch (modelProvider) {
            case MiniMax:
                return new MiniMaxLLMClient(miniMaxModel, miniMaxAPIKey, miniMaxBaseUrl, modelProvider.name());
            default:
                return null;
        }
    }

}