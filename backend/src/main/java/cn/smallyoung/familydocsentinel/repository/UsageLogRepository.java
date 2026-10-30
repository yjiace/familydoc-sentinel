package cn.smallyoung.familydocsentinel.repository;

import cn.smallyoung.familydocsentinel.entity.UsageLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UsageLogRepository extends JpaRepository<UsageLogEntity, Long> {

    Page<UsageLogEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(u.totalTokens), 0) FROM UsageLogEntity u WHERE u.userId = :userId AND u.createdAt >= :startTime")
    long sumTotalTokensByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);

    @Query("SELECT COALESCE(SUM(u.searchCount), 0) FROM UsageLogEntity u WHERE u.userId = :userId AND u.createdAt >= :startTime")
    long sumSearchCountByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);
}

