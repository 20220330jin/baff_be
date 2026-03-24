package com.sa.baff.api;

import com.sa.baff.domain.Notice;
import com.sa.baff.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeRestController {

    private final NoticeRepository noticeRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getNotices() {
        List<Map<String, Object>> notices = noticeRepository.findAllByOrderByRegDateTimeDesc()
                .stream()
                .filter(Notice::getIsActive)
                .map(n -> Map.<String, Object>of(
                        "id", n.getId(),
                        "title", n.getTitle(),
                        "regDateTime", n.getRegDateTime().toString()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(notices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getNoticeDetail(@PathVariable Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다."));
        return ResponseEntity.ok(Map.of(
                "id", notice.getId(),
                "title", notice.getTitle(),
                "content", notice.getContent(),
                "regDateTime", notice.getRegDateTime().toString()
        ));
    }
}
