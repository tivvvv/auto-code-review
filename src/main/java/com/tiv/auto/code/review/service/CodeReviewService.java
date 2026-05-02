package com.tiv.auto.code.review.service;

public interface CodeReviewService {

    String reviewCode(String diffText, String commitMessage);

    String detectCodeLanguage(String text);

    int parseCodeReviewScore(String reviewText);

}