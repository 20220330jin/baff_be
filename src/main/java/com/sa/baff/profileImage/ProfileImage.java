package com.sa.baff.profileImage;

import java.util.Arrays;
import java.util.List;

public class ProfileImage {
    // 후회일기 서비스 분위기에 맞춘 20개의 DiceBear 프로필 이미지 URL 목록
    public static final List<String> PROFILE_IMAGE_URLS = Arrays.asList(
            // Style 1: avataaars (클래식)
            "https://api.dicebear.com/7.x/avataaars/svg?seed=Regret&backgroundColor=F0C3CB",
            "https://api.dicebear.com/7.x/avataaars/svg?seed=Diary&backgroundColor=F0C3CB",
            "https://api.dicebear.com/7.x/avataaars/svg?seed=Hope&backgroundColor=F0C3CB",
            "https://api.dicebear.com/7.x/avataaars/svg?seed=Growth&backgroundColor=F0C3CB",

            // Style 2: personas (감성적인 인물)
            "https://api.dicebear.com/7.x/personas/svg?seed=Reflection&backgroundColor=F0C3CB",
            "https://api.dicebear.com/7.x/personas/svg?seed=Comfort&backgroundColor=F0C3CB",
            "https://api.dicebear.com/7.x/personas/svg?seed=Past&backgroundColor=F0C3CB",
            "https://api.dicebear.com/7.x/personas/svg?seed=Tomorrow&backgroundColor=F0C3CB",

            // Style 3: fun-emoji (재미)
            "https://api.dicebear.com/7.x/fun-emoji/svg?seed=Joyful&backgroundColor=F0C3CB",
            "https://api.dicebear.com/7.x/fun-emoji/svg?seed=Sad&backgroundColor=F0C3CB",
            "https://api.dicebear.com/7.x/fun-emoji/svg?seed=Sorrow&backgroundColor=F0C3CB",
            "https://api.dicebear.com/7.x/fun-emoji/svg?seed=Lesson&backgroundColor=F0C3CB",

            // Style 4: pixel-art (레트로/깔끔)
            "https://api.dicebear.com/7.x/pixel-art/svg?seed=Wisdom&backgroundColor=F0C3CB",
            "https://api.dicebear.com/7.x/pixel-art/svg?seed=Memo&backgroundColor=F0C3CB",
            "https://api.dicebear.com/7.x/pixel-art/svg?seed=Record&backgroundColor=F0C3CB",
            "https://api.dicebear.com/7.x/pixel-art/svg?seed=Begin&backgroundColor=F0C3CB",

            // Style 5: bottts (귀여운 봇)
            "https://api.dicebear.com/7.x/bottts/svg?seed=User1&backgroundColor=F0C3CB",
            "https://api.dicebear.com/7.x/bottts/svg?seed=User2&backgroundColor=F0C3CB",
            "https://api.dicebear.com/7.x/bottts/svg?seed=User3&backgroundColor=F0C3CB",
            "https://api.dicebear.com/7.x/bottts/svg?seed=User4&backgroundColor=F0C3CB"
    );
}