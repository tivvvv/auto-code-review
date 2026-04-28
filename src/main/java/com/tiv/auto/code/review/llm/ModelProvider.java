package com.tiv.auto.code.review.llm;

public enum ModelProvider {

    MiniMax;

    public static ModelProvider getEnumByName(String name) {
        for (ModelProvider modelProvider : ModelProvider.values()) {
            if (modelProvider.name().equalsIgnoreCase(name)) {
                return modelProvider;
            }
        }
        return null;
    }

}