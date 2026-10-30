package cn.smallyoung.familydocsentinel.repository;

import cn.smallyoung.familydocsentinel.entity.UserUsageStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserUsageStatRepository extends JpaRepository<UserUsageStatEntity, Long> {

    Optional<UserUsageStatEntity> findByUserId(Long userId);
}

