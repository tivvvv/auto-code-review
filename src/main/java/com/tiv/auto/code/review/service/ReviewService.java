package com.tiv.auto.code.review.service;

import com.tiv.auto.code.review.constants.AutoCodeReviewConstants;
import com.tiv.auto.code.review.entity.PullRequestReviewLog;
import com.tiv.auto.code.review.entity.PushReviewLog;
import com.tiv.auto.code.review.llm.LLMClient;
import com.tiv.auto.code.review.llm.LLMFactory;
import com.tiv.auto.code.review.repository.PullRequestReviewLogRepository;
import com.tiv.auto.code.review.repository.PushReviewLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    @Autowired
    private LLMFactory llmFactory;

    @Autowired
    private PullRequestReviewLogRepository pullRequestReviewLogRepository;

    @Autowired
    private PushReviewLogRepository pushReviewLogRepository;

    public String reviewCode(String diffText) {
        // 1. 获取当前配置的客户端
        LLMClient client = llmFactory.getClient();

        // 2. 构造消息
        List<Map<String, String>> messages = List.of(
                Map.of(AutoCodeReviewConstants.ROLE, AutoCodeReviewConstants.SYSTEM,
                        AutoCodeReviewConstants.CONTENT, "你是代码评审专家,请按规范评审下面的代码变更,指出问题并给出建议."),
                Map.of(AutoCodeReviewConstants.ROLE, AutoCodeReviewConstants.USER,
                        AutoCodeReviewConstants.CONTENT, diffText));

        // 3. 调用模型
        return client.chat(messages);
    }

    public void savePullRequestReview(PullRequestReviewLog log) {
        pullRequestReviewLogRepository.save(log);
    }

    public void savePushReview(PushReviewLog log) {
        pushReviewLogRepository.save(log);
    }

    public List<PullRequestReviewLog> queryPullRequestReviews(String author, String project) {
        List<String> authors = author != null ? List.of(author) : null;
        List<String> projects = project != null ? List.of(project) : null;
        return pullRequestReviewLogRepository.queryByFilters(authors, projects, null, null);
    }

    public List<String> getAllAuthors() {
        return pullRequestReviewLogRepository.queryDistinctAuthors();
    }

    public List<String> getAllProjects() {
        return pullRequestReviewLogRepository.queryDistinctProjectNames();
    }

}