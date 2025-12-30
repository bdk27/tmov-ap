package com.brian.tmov.service.impl;

import com.brian.tmov.dao.entity.MemberEntity;
import com.brian.tmov.dao.entity.RoleEntity;
import com.brian.tmov.dao.repository.MemberRepository;
import com.brian.tmov.dao.repository.RoleRepository;
import com.brian.tmov.dto.request.AuthRequest;
import com.brian.tmov.dto.request.UpdateProfileRequest;
import com.brian.tmov.dto.response.AuthResponse;
import com.brian.tmov.security.JwtUtil;
import com.brian.tmov.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Transactional
    @Override
    public void register(AuthRequest request) {
        // 使用 email 作為唯一標識
        if (memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("此 Email 已被註冊");
        }

        MemberEntity member = new MemberEntity();

        member.setEmail(request.email());
        member.setPasswordHash(passwordEncoder.encode(request.password()));

        String defaultName = request.email().split("@")[0];
        member.setDisplayName(defaultName);
        member.setPictureUrl("https://api.dicebear.com/7.x/initials/svg?seed=" + defaultName);

        // 設定預設角色
        RoleEntity userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new RoleEntity("ROLE_USER")));
        member.addRole(userRole);

        memberRepository.save(member);
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        // 驗證 email
        MemberEntity member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("登入失敗：找不到此帳號"));

        // 驗證 password
        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BadCredentialsException("登入失敗：密碼錯誤");
        }

        // 組合角色字串
        String roleNames = member.getRoles().stream()
                .map(RoleEntity::getName).collect(Collectors.joining(","));

        // 產生 Token
        String token = jwtUtil.generateToken(member.getEmail(), roleNames, request.isRememberMe());

        return new AuthResponse(
                token,
                member.getEmail(),
                member.getDisplayName(),
                member.getPictureUrl(),
                roleNames,
                member.getCreatedAt()
        );
    }

    @Override
    public AuthResponse getMe(String email) {
        MemberEntity member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("找不到會員資料"));

        String roleNames = member.getRoles().stream()
                .map(RoleEntity::getName).collect(Collectors.joining(","));

        return new AuthResponse(
                null,
                member.getEmail(),
                member.getDisplayName(),
                member.getPictureUrl(),
                roleNames,
                member.getCreatedAt()
        );
    }

    @Transactional
    @Override
    public AuthResponse updateProfile(String email, UpdateProfileRequest request) {
        MemberEntity member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("找不到會員資料"));

        // 更新暱稱
        if (request.displayName() != null && !request.displayName().isBlank()) {
            member.setDisplayName(request.displayName());
        }

        // 更新頭像
        if (request.pictureUrl() != null && !request.pictureUrl().isBlank()) {
            member.setPictureUrl(request.pictureUrl());
        }

        // 更新密碼
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            // 檢查是否提供舊密碼
            if (request.oldPassword() == null || request.oldPassword().isBlank()) {
                throw new IllegalArgumentException("修改密碼時，必須提供舊密碼");
            }
            // 驗證舊密碼是否正確
            if (!passwordEncoder.matches(request.oldPassword(), member.getPasswordHash())) {
                throw new BadCredentialsException("舊密碼不正確，無法修改密碼");
            }
            // 設定新密碼
            member.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }

        // 儲存更新
        memberRepository.save(member);

        // 回傳更新後的資訊
        String roleNames = member.getRoles().stream()
                .map(RoleEntity::getName).collect(Collectors.joining(","));

        return new AuthResponse(
                null,
                member.getEmail(),
                member.getDisplayName(),
                member.getPictureUrl(),
                roleNames,
                member.getCreatedAt()
        );
    }
}