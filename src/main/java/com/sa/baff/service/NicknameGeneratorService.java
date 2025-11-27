package com.sa.baff.service;

import com.sa.baff.domain.UserB;
import com.sa.baff.profileImage.ProfileImage;
import com.sa.baff.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class NicknameGeneratorService {

    private final UserRepository userRepository;

    // ... (ADJECTIVES, NOUNS, RANDOM 정의는 이전과 동일) ...
    private static final List<String> ADJECTIVES = Arrays.asList(
            "고민 많은", "잠 못 이루는", "반성하는", "다짐하는", "용기 있는",
            "따뜻한", "공감하는", "어제의", "오늘의", "내일의", "지나간", "희미한"
    );

    private static final List<String> NOUNS = Arrays.asList(
            // 친근함 & 캐릭터
            "고양이", "강아지", "부엉이", "수달", "토끼", "다람쥐", "곰돌이", "펭귄", "친구", "짝꿍", "동행자", "여행자",

            // 성찰 & 기록
            "일기장", "기록자", "회고록", "책갈피", "문장", "숨결", "흔적", "조각", "발자국", "기억",

            // 위로 & 희망
            "위로", "희망", "용기", "다짐", "새싹", "내일", "별빛", "등대", "달님", "햇살", "나침반", "온기",

            // 상징 & 성장
            "나무", "씨앗", "바람개비", "오르골", "시간", "교훈", "깨달음", "성장", "강물", "언덕"
    );

    private static final Random RANDOM = new Random();

    /**
     * 고유한 랜덤 닉네임을 생성하여 UserEntity에 설정 후 DB에 저장합니다.
     * 트랜잭션으로 묶여 충돌 시 예외가 발생하며 롤백됩니다.
     * @param user 닉네임과 이미지를 설정해야 하는 UserEntity 객체 (새로운 유저)
     */
    @Transactional // 닉네임 생성 및 DB 저장 과정을 트랜잭션으로 묶음
    public void generateUniqueNicknameAndSave(UserB user) {

        // 1. 닉네임 생성
        String adjective = ADJECTIVES.get(RANDOM.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(RANDOM.nextInt(NOUNS.size()));
        String baseNickname = adjective + " " + noun;

        // **중요:** 트랜잭션 내에서 카운트 및 저장이 이루어져야 동시성 문제가 최소화됩니다.
        long existingCount = userRepository.countBynicknameStartingWith(baseNickname);
        long nextIndex = existingCount + 1;
        String uniqueNickname = String.format("%s %d", baseNickname, nextIndex);

        // 2. UserEntity에 닉네임 설정
        user.setNickname(uniqueNickname);

        // 3. DB에 저장 시도
        // *닉네임 충돌(경쟁 조건) 시 DataIntegrityViolationException 발생 및 롤백됨*
        userRepository.save(user);
    }

    /**
     * ProfileImage에 정의된 URL 중 하나를 랜덤으로 선택합니다.
     */
    public String getRandomProfileImageUrl() {
        int randomIndex = RANDOM.nextInt(ProfileImage.PROFILE_IMAGE_URLS.size());
        return ProfileImage.PROFILE_IMAGE_URLS.get(randomIndex);
    }
}
