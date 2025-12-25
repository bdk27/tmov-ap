package com.brian.tmov.service.impl;

import com.brian.tmov.dao.entity.MemberEntity;
import com.brian.tmov.dao.entity.RoleEntity;
import com.brian.tmov.dao.repository.MemberRepository;
import com.brian.tmov.dao.repository.RoleRepository;
import com.brian.tmov.dto.request.AuthRequest;
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
    public AuthResponse register(AuthRequest request) {
        // 使用 email 作為唯一標識
        if (memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("此 Email 已被註冊");
        }

        MemberEntity member = new MemberEntity();
        member.setEmail(request.email());
        member.setDisplayName(request.email().split("@")[0]); // 預設顯示名稱
        member.setPasswordHash(passwordEncoder.encode(request.password()));

        // 設定預設角色
        RoleEntity userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new RoleEntity("ROLE_USER")));
        member.addRole(userRole);

        memberRepository.save(member);

        // 註冊後直接發 Token
        String roleNames = member.getRoles().stream()
                .map(RoleEntity::getName).collect(Collectors.joining(","));
        String token = jwtUtil.generateToken(member.getEmail(), roleNames);

        return new AuthResponse(
                token,
                member.getEmail(),
                member.getDisplayName(),
                member.getPictureUrl(),
                roleNames
        );
    }

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
        String token = jwtUtil.generateToken(member.getEmail(), roleNames);

        return new AuthResponse(
                token,
                member.getEmail(),
                member.getDisplayName(),
                member.getPictureUrl(),
                roleNames
        );
    }
}