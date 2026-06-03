package cn.edu.cn.javadiet.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import cn.edu.cn.javadiet.model.entity.User;
import cn.edu.cn.javadiet.model.entity.UserLoginSession;
import cn.edu.cn.javadiet.model.enums.UserRole;
import cn.edu.cn.javadiet.repository.UserLoginSessionRepository;
import cn.edu.cn.javadiet.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AdminAuthInterceptorTests {

    @Test
    void rejectsMissingToken() throws Exception {
        AdminAuthInterceptor interceptor = new AdminAuthInterceptor(
                Mockito.mock(UserLoginSessionRepository.class),
                Mockito.mock(UserRepository.class));

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean allowed = interceptor.preHandle(new MockHttpServletRequest(), response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
    }

    @Test
    void allowsActiveAdminSession() throws Exception {
        UserLoginSessionRepository sessionRepository = Mockito.mock(UserLoginSessionRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        UserLoginSession session = UserLoginSession.builder().userId(1L).token("token").active(true).build();
        User admin = User.builder().id(1L).role(UserRole.ADMIN).build();
        when(sessionRepository.findByTokenAndActiveTrue("token")).thenReturn(Optional.of(session));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        AdminAuthInterceptor interceptor = new AdminAuthInterceptor(sessionRepository, userRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertTrue(allowed);
    }

    @Test
    void rejectsActiveNonAdminSession() throws Exception {
        UserLoginSessionRepository sessionRepository = Mockito.mock(UserLoginSessionRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        UserLoginSession session = UserLoginSession.builder().userId(2L).token("token").active(true).build();
        User user = User.builder().id(2L).role(UserRole.USER).build();
        when(sessionRepository.findByTokenAndActiveTrue("token")).thenReturn(Optional.of(session));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        AdminAuthInterceptor interceptor = new AdminAuthInterceptor(sessionRepository, userRepository);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/foods/nutrition/pending");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(403, response.getStatus());
    }
}
