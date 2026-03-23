package com.sa.baff.repository;

import com.sa.baff.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByRegDateTimeDesc();
}
