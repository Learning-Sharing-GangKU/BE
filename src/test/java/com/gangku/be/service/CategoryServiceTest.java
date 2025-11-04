package com.gangku.be.service;

import com.gangku.be.domain.Category;
import com.gangku.be.dto.category.CategoryDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.ErrorCode;
import com.gangku.be.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class CategoryServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createCategory_정상등록() {
        // given
        String name = "운동";
        when(categoryRepository.findByName(name)).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Category saved = categoryService.createCategory(name);

        // then
        assertThat(saved.getName()).isEqualTo(name);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void createCategory_중복오류() {
        // given
        String name = "음악";
        when(categoryRepository.findByName(name)).thenReturn(Optional.of(new Category()));

        // then
        assertThrows(IllegalArgumentException.class, () -> categoryService.createCategory(name));
    }

    @Test
    void getAllCategories_조회() {
        // given
        List<Category> categories = List.of(
                new Category(1L, "음악", null, null),
                new Category(2L, "운동", null, null)
        );
        when(categoryRepository.findAll()).thenReturn(categories);

        // when
        List<CategoryDto> result = categoryService.getAllCategories();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("음악");
    }

    @Test
    void getAllCategories_빈목록_예외() {
        // given
        when(categoryRepository.findAll()).thenReturn(List.of()); // 빈 리스트 반환

        // when & then
        CustomException ex = assertThrows(CustomException.class, () ->
                categoryService.getAllCategories());

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CATEGORIES_NOT_FOUND);
        assertThat(ex.getMessage()).contains("카테고리 목록을 찾을 수 없습니다");
    }
}