package com.gangku.be.service.category;

import com.gangku.be.domain.Category;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.service.CategoryService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetAllCategoriesServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category mockCategory(String name) {
        Category category = mock(Category.class);
        when(category.getName()).thenReturn(name);
        return category;
    }

    // =========================================================
    // 1. 정상 케이스
    // =========================================================

    @Test
    @DisplayName("여러 개의 카테고리 → 이름 리스트를 오름차순 그대로 반환")
    void getAllCategories_withMultipleCategories_returnsNameListInOrder() {
        // given
        Category backend = mockCategory("Backend");
        Category frontend = mockCategory("Frontend");
        Category infra = mockCategory("Infra");

        when(categoryRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(backend, frontend, infra));

        // when
        List<String> result = categoryService.getAllCategories();

        // then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(List.of("Backend", "Frontend", "Infra"), result);
    }

    @Test
    @DisplayName("카테고리가 1개일 때 → 해당 이름만 담긴 리스트 반환")
    void getAllCategories_withSingleCategory_returnsSingleElementList() {
        // given
        Category backend = mockCategory("Backend");

        when(categoryRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(backend));

        // when
        List<String> result = categoryService.getAllCategories();

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(List.of("Backend"), result);
    }

    // =========================================================
    // 2. 경계 케이스
    // =========================================================

    @Test
    @DisplayName("카테고리가 하나도 없을 때 → 빈 리스트 반환")
    void getAllCategories_withNoCategory_returnsEmptyList() {
        // given
        when(categoryRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of());

        // when
        List<String> result = categoryService.getAllCategories();

        // then
        assertNotNull(result, "null 이 아니라 빈 리스트를 반환해야 한다.");
        assertTrue(result.isEmpty(), "리스트가 비어 있어야 한다.");
    }

    @Test
    @DisplayName("카테고리 이름에 공백/특수문자가 포함된 경우 → 그대로 반환")
    void getAllCategories_withCategoryNamesContainingSpaces_returnsNamesAsIs() {
        // given
        Category webDev = mockCategory("Web Dev");
        Category dataScience = mockCategory("Data-Science");
        Category aiMl = mockCategory("AI/ML");

        when(categoryRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(webDev, dataScience, aiMl));

        // when
        List<String> result = categoryService.getAllCategories();

        // then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(List.of("Web Dev", "Data-Science", "AI/ML"), result);
    }

    // =========================================================
    // 3. 예외 케이스
    // =========================================================

    @Test
    @DisplayName("Repository에서 예외 발생 시 → 예외를 그대로 전파")
    void getAllCategories_whenRepositoryThrowsException_propagatesException() {
        // given
        when(categoryRepository.findAllByOrderByNameAsc())
                .thenThrow(new RuntimeException("DB error"));

        // when & then
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> categoryService.getAllCategories());

        assertEquals("DB error", ex.getMessage());
    }
}
