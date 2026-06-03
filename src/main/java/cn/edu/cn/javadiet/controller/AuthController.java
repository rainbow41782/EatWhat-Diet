package cn.edu.cn.javadiet.controller;

import cn.edu.cn.javadiet.common.ApiResponse;
import cn.edu.cn.javadiet.model.dto.LoginRequest;
import cn.edu.cn.javadiet.model.dto.LoginResponse;
import cn.edu.cn.javadiet.model.dto.LogoutRequest;
import cn.edu.cn.javadiet.model.dto.RegisterRequest;
import cn.edu.cn.javadiet.model.entity.User;
import cn.edu.cn.javadiet.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.loginWithSession(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody LogoutRequest request) {
        userService.logout(request.getToken());
        return ResponseEntity.ok(ApiResponse.ok("logged out", null));
    }
}
