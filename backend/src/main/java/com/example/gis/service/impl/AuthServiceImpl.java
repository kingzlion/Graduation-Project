package com.example.gis.service.impl;

import com.example.gis.entity.AdminUser;
import com.example.gis.repository.AdminUserRepository;
import com.example.gis.service.AuthService;
import com.example.gis.utils.PasswordUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final AdminUserRepository adminUserRepository;

    public AuthServiceImpl(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public Optional<AdminUser> login(String account, String password) {
        return adminUserRepository.findById(account)
                .filter(user -> user.getPasswordHash().equals(PasswordUtils.sha256(password)));
    }

    @Override
    public AdminUser register(String account, String displayName, String email, String password) {
        if (adminUserRepository.existsById(account)) {
            throw new IllegalArgumentException("账号已存在");
        }
        if (adminUserRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("邮箱已被使用");
        }
        AdminUser user = new AdminUser();
        user.setAccount(account);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setPasswordHash(PasswordUtils.sha256(password));
        user.setCreatedAt(LocalDateTime.now().toString());
        return adminUserRepository.save(user);
    }

    @Override
    public AdminUser resetPassword(String account, String email, String newPassword) {
        AdminUser user = adminUserRepository.findById(account)
                .orElseThrow(() -> new IllegalArgumentException("账号不存在"));
        if (!user.getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("账号和邮箱不匹配");
        }
        user.setPasswordHash(PasswordUtils.sha256(newPassword));
        return adminUserRepository.save(user);
    }

    @Override
    public Optional<AdminUser> findByAccount(String account) {
        return adminUserRepository.findById(account);
    }

    @Override
    public AdminUser ensureDefaultAdmin() {
        return adminUserRepository.findById("admin").orElseGet(() -> {
            AdminUser user = new AdminUser();
            user.setAccount("admin");
            user.setDisplayName("系统管理员");
            user.setEmail("admin@xagis.local");
            user.setPasswordHash(PasswordUtils.sha256("admin123"));
            user.setCreatedAt(LocalDateTime.now().toString());
            return adminUserRepository.save(user);
        });
    }
}
