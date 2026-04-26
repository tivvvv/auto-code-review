package com.tiv.auto.code.review.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * push 评审记录
 *
 * @TableName push_review_log
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "push_review_log")
public class PushReviewLog {

    /**
     * id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 项目名
     */
    @Column(name = "project_name")
    private String projectName;

    /**
     * 作者
     */
    @Column(name = "author")
    private String author;

    /**
     * 分支
     */
    @Column(name = "branch")
    private String branch;

    /**
     * 新增代码行数
     */
    @Column(name = "additions")
    @Builder.Default
    private Integer additions = 0;

    /**
     * 删除代码行数
     */
    @Column(name = "deletions")
    @Builder.Default
    private Integer deletions = 0;

    /**
     * commit 链接
     */
    @Column(name = "url")
    private String url;

    /**
     * 提交信息
     */
    @Column(name = "commit_message")
    private String commitMessage;

    /**
     * 代码评分
     */
    @Column(name = "score")
    private Integer score;

    /**
     * 评审结果
     */
    @Column(name = "review_result")
    private String reviewResult;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private Date createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private Date updatedAt;

}