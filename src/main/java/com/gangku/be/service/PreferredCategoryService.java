package com.gangku.be.service;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.PreferredCategory;
import com.gangku.be.domain.User;
import com.gangku.be.dto.preferred.PreferredCategoryResponseDto;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.PreferredCategoryRepository;
import com.gangku.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // 생성자 주입 자동 생성
public class PreferredCategoryService {

    private final PreferredCategoryRepository preferredCategoryRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    /**
     * 선호 카테고리 저장
     * - 사용자가 선택한 카테고리 이름들을 기반으로 PreferredCategory 엔티티들을 생성
     *
     * @param user 사용자 엔티티
     * @param categoryNames 카테고리 이름 리스트 (최대 3개)
     */
    public PreferredCategoryResponseDto setPreferredCategories(User user, List<String> categoryNames){
        // 카테고리 이름을 기반으로 실제 Category 엔티티 리스트 조회
        List<Category> categories = categoryNames.stream()
                .map(name -> categoryRepository.findByName(name)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다: " + name)))
                .collect(Collectors.toList());

        // 기존에 저장된 선호 카테고리 삭제 (중복 방지)
        preferredCategoryRepository.deleteByUser(user);

        // 새로운 PreferredCategory 엔티티 저장
        for (Category category : categories) {
            PreferredCategory preferred = new PreferredCategory();
            preferred.setUser(user);
            preferred.setCategory(category);
            preferredCategoryRepository.save(preferred);
        }
        // 응답용 카테고리 이름 리스트 생성
        List<String> categoryNameList = categories.stream()
                .map(Category::getName)
                .collect(Collectors.toList());

        return new PreferredCategoryResponseDto(categoryNameList);
    }

    /**
     * 사용자의 선호 카테고리 목록 조회
     *
     * @param userId 사용자 ID
     * @return 카테고리 이름 리스트
     */
    public List<String> getPreferredCategoryNames(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return preferredCategoryRepository.findByUser(user).stream()
                .map(pc -> pc.getCategory().getName())
                .collect(Collectors.toList());
    }
}