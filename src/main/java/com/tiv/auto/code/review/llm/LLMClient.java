package com.tiv.auto.code.review.llm;

import java.util.List;
import java.util.Map;

public interface LLMClient {

    String chat(List<Map<String, String>> msgs);

}