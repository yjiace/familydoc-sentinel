package cn.smallyoung.familydocsentinel.repository;

import cn.smallyoung.familydocsentinel.entity.UserHistoryTaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserHistoryTaskRepository extends JpaRepository<UserHistoryTaskEntity, Long> {

    Optional<UserHistoryTaskEntity> findByTaskId(String taskId);

    Optional<UserHistoryTaskEntity> findByTaskIdAndUserId(String taskId, Long userId);

    Page<UserHistoryTaskEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    void deleteByTaskIdAndUserId(String taskId, Long userId);

    void deleteAllByUserId(Long userId);

    long countByUserId(Long userId);
}

