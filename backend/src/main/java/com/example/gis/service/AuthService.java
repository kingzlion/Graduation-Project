package com.example.gis.service;

import com.example.gis.entity.AdminUser;

import java.util.Optional;

public interface AuthService {

    Optional<AdminUser> login(String account, String password);

    AdminUser register(String account, String displayName, String email, String password);

    AdminUser resetPassword(String account, String email, String newPassword);

    Optional<AdminUser> findByAccount(String account);

    AdminUser ensureDefaultAdmin();
}
