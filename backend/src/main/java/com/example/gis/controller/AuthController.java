package com.example.gis.controller;

import com.example.gis.entity.AdminUser;
import com.example.gis.service.AuthService;
import com.example.gis.utils.Result;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    public static final String SESSION_KEY = "LOGIN_ACCOUNT";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestParam String account,
                                             @RequestParam String password,
                                             HttpSession session) {
        return authService.login(account, password)
                .map(user -> {
                    session.setAttribute(SESSION_KEY, user.getAccount());
                    return Result.success(buildProfile(user));
                })
                .orElseGet(() -> Result.error("账号或密码错误", 401));
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestParam String account,
                                                @RequestParam String displayName,
                                                @RequestParam String email,
                                                @RequestParam String password) {
        try {
            AdminUser user = authService.register(account, displayName, email, password);
            return Result.success("注册成功", buildProfile(user));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage(), 400);
        }
    }

    @PostMapping("/reset-password")
    public Result<Map<String, Object>> resetPassword(@RequestParam String account,
                                                     @RequestParam String email,
                                                     @RequestParam String newPassword) {
        try {
            AdminUser user = authService.resetPassword(account, email, newPassword);
            return Result.success("密码已重置", buildProfile(user));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage(), 400);
        }
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me(HttpSession session) {
        Object account = session.getAttribute(SESSION_KEY);
        if (account == null) {
            return Result.error("未登录", 401);
        }
        return authService.findByAccount(account.toString())
                .map(user -> Result.success(buildProfile(user)))
                .orElseGet(() -> Result.error("未登录", 401));
    }

    @PostMapping("/logout")
    public Result<String> logout(HttpSession session) {
        session.invalidate();
        return Result.success("已退出登录");
    }

    private Map<String, Object> buildProfile(AdminUser user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("account", user.getAccount());
        profile.put("displayName", user.getDisplayName());
        profile.put("email", user.getEmail());
        profile.put("createdAt", user.getCreatedAt());
        if ("admin".equals(user.getAccount())) {
            profile.put("user_role", 3);
        } else {
            profile.put("user_role", 1);
        }
        return profile;
    }
}
