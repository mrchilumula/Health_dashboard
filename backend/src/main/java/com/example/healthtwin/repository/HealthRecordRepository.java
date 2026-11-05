package com.example.healthtwin.repository;

import com.example.healthtwin.entity.HealthRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

    Optional<HealthRecord> findByUserIdAndDedupeKey(String userId, String dedupeKey);

    Optional<HealthRecord> findFirstByUserIdOrderByEventTimestampDesc(String userId);

    Page<HealthRecord> findByUserIdOrderByEventTimestampDesc(String userId, Pageable pageable);

    List<HealthRecord> findTop10ByUserIdOrderByEventTimestampDesc(String userId);

    @Query("SELECT r FROM HealthRecord r WHERE r.userId = :userId AND r.eventTimestamp >= :fromTimestamp ORDER BY r.eventTimestamp ASC")
    List<HealthRecord> findByUserIdAndEventTimestampAfterOrderByEventTimestampAsc(@Param("userId") String userId, @Param("fromTimestamp") Long fromTimestamp);

    @Query("SELECT COUNT(r) FROM HealthRecord r WHERE r.userId = :userId")
    long countByUserId(@Param("userId") String userId);
}
