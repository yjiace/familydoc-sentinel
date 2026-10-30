package cn.smallyoung.familydocsentinel.repository;

import cn.smallyoung.familydocsentinel.entity.UsageLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsageLogRepository extends JpaRepository<UsageLogEntity, Long> {

    Page<UsageLogEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

