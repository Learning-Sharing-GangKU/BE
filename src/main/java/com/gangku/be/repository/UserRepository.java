// src/main/java/com/gangku/BE/repository/UserRepository.java

package com.gangku.be.repository;

import com.gangku.be.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email); //이메일을 기준으로 User 조회 메서드
    boolean existsByNickname(String nickname); // 닉네임 존재 여부 확인 메서드
    boolean existsByEmail(String email); // 이메일 존재 여부 확인 메서드
    Optional<User> findById(Long id);
}