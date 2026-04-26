package com.tiv.auto.code.review.service;

import com.tiv.auto.code.review.entity.PullRequestReviewLog;
import com.tiv.auto.code.review.entity.PushReviewLog;
import com.tiv.auto.code.review.repository.PullRequestReviewLogRepository;
import com.tiv.auto.code.review.repository.PushReviewLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private PullRequestReviewLogRepository pullRequestReviewLogRepository;

    @Autowired
    private PushReviewLogRepository pushReviewLogRepository;

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