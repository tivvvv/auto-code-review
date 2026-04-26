package com.tiv.auto.code.review.repository;

import com.tiv.auto.code.review.entity.PullRequestReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface PullRequestReviewLogRepository extends JpaRepository<PullRequestReviewLog, Long> {

    @Query("SELECT pr FROM PullRequestReviewLog pr WHERE " +
            "(:authors IS NULL OR pr.author IN :authors) AND " +
            "(:projectNames IS NULL OR pr.projectName IN :projectNames) AND " +
            "(:updatedAtGte IS NULL OR pr.updatedAt >= :updatedAtGte) AND " +
            "(:updatedAtLte IS NULL OR pr.updatedAt <= :updatedAtLte) " +
            "ORDER BY pr.updatedAt DESC")
    List<PullRequestReviewLog> queryByFilters(@Param("authors") List<String> authors,
                                              @Param("projectNames") List<String> projectNames,
                                              @Param("updatedAtGte") Date updatedAtGte,
                                              @Param(value = "updatedAtLte") Date updatedAtLte);

    @Query("SELECT DISTINCT pr.author FROM PullRequestReviewLog pr ORDER BY pr.author")
    List<String> queryDistinctAuthors();

    @Query("SELECT DISTINCT pr.projectName FROM PullRequestReviewLog pr ORDER BY pr.projectName")
    List<String> queryDistinctProjectNames();

}