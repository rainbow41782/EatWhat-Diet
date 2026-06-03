package cn.edu.cn.javadiet.config;

import cn.edu.cn.javadiet.common.ApiResponse;
import cn.edu.cn.javadiet.model.entity.User;
import cn.edu.cn.javadiet.model.entity.UserLoginSession;
import cn.edu.cn.javadiet.model.enums.UserRole;
import cn.edu.cn.javadiet.repository.UserLoginSessionRepository;
import cn.edu.cn.javadiet.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final UserLoginSessionRepository userLoginSessionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminAuthInterceptor(
            UserLoginSessionRepository userLoginSessionRepository,
            UserRepository userRepository) {
        this.userLoginSessionRepository = userLoginSessionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String token = readBearerToken(request);
        if (token == null || token.isBlank()) {
            writeError(response, HttpStatus.UNAUTHORIZED, "admin token is required");
            return false;
        }

        Optional<UserLoginSession> session = userLoginSessionRepository.findByTokenAndActiveTrue(token);
        if (session.isEmpty()) {
            writeError(response, HttpStatus.UNAUTHORIZED, "admin token is invalid");
            return false;
        }

        Optional<User> user = userRepository.findById(session.get().getUserId());
        if (user.isEmpty() || user.get().getRole() != UserRole.ADMIN) {
            writeError(response, HttpStatus.FORBIDDEN, "admin role is required");
            return false;
        }
        return true;
    }

    private static String readBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || header.isBlank()) {
            return null;
        }
        if (header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return header.substring(7).trim();
        }
        return header.trim();
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(message));
    }
}
