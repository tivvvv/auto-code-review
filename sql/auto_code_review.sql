CREATE DATABASE IF NOT EXISTS auto_code_review;

USE auto_code_review;

CREATE TABLE IF NOT EXISTS pull_request_review_log
(
    id             BIGINT                             NOT NULL AUTO_INCREMENT COMMENT 'id',
    project_name   VARCHAR(256)                       NOT NULL COMMENT '项目名',
    author         VARCHAR(256)                       NOT NULL COMMENT '作者',
    source_branch  VARCHAR(256)                       NOT NULL COMMENT '源分支',
    target_branch  VARCHAR(256)                       NOT NULL COMMENT '目标分支',
    additions      INT      DEFAULT 0                 NOT NULL COMMENT '新增代码行数',
    deletions      INT      DEFAULT 0                 NOT NULL COMMENT '删除代码行数',
    url            VARCHAR(2048)                      NOT NULL COMMENT 'pr 链接',
    commit_message TEXT                               NULL COMMENT '提交信息',
    score          INT                                NULL COMMENT '代码评分',
    review_result  TEXT                               NULL COMMENT '评审结果',
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_project_name (project_name),
    INDEX idx_author (author)
) COMMENT 'pr 评审记录';

CREATE TABLE IF NOT EXISTS push_review_log
(
    id             BIGINT                             NOT NULL AUTO_INCREMENT COMMENT 'id',
    project_name   VARCHAR(256)                       NOT NULL COMMENT '项目名',
    author         VARCHAR(256)                       NOT NULL COMMENT '作者',
    branch         VARCHAR(256)                       NOT NULL COMMENT '分支',
    additions      INT      DEFAULT 0                 NOT NULL COMMENT '新增代码行数',
    deletions      INT      DEFAULT 0                 NOT NULL COMMENT '删除代码行数',
    url            VARCHAR(2048)                      NOT NULL COMMENT 'commit 链接',
    commit_message TEXT                               NULL COMMENT '提交信息',
    score          INT                                NULL COMMENT '代码评分',
    review_result  TEXT                               NULL COMMENT '评审结果',
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_project_name (project_name),
    INDEX idx_author (author),
    INDEX idx_branch (branch)
) COMMENT 'push 评审记录';
