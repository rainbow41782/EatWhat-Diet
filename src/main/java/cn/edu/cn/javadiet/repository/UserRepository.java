package cn.edu.cn.javadiet.repository;

import cn.edu.cn.javadiet.model.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsernameIgnoreCase(String username);

    Optional<User> findByUsernameIgnoreCase(String username);

    List<User> findByUsernameContainingIgnoreCaseOrNicknameContainingIgnoreCaseOrPhoneContainingOrEmailContainingIgnoreCase(
            String username,
            String nickname,
            String phone,
            String email);
}
