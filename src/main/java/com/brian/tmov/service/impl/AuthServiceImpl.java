package com.brian.tmov.service.impl;

import com.brian.tmov.dao.entity.MemberEntity;
import com.brian.tmov.dao.entity.RoleEntity;
import com.brian.tmov.dao.repository.MemberRepository;
import com.brian.tmov.dao.repository.RoleRepository;
import com.brian.tmov.dto.request.AuthRequest;
import com.brian.tmov.dto.request.GoogleLoginRequest;
import com.brian.tmov.dto.request.UpdateProfileRequest;
import com.brian.tmov.dto.response.AuthResponse;
import com.brian.tmov.security.JwtUtil;
import com.brian.tmov.service.AuthService;
import com.brian.tmov.service.FileService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
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
    private FileService fileService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Value("${google.client-id}")
    private String googleClientId;

    private static final String DEFAULT_AVATAR_PREFIX = "https://api.dicebear.com";

    private static final String DEFAULT_AVATAR_STYLE = "initials";

    private static final String ADMIN_EMAIL = "briansam195@gmail.com";

    @Transactional
    @Override
    public void register(AuthRequest request) {
        // 使用 email 作為唯一標識
        if (memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("此 Email 已被註冊");
        }
        createMember(request.email(), request.password(), null, null);
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
                member.getGender(),
                member.getBirthDate(),
                member.getPhone(),
                member.getAddress(),
                member.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = verifyGoogleToken(request.idToken());

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        // 2. 檢查會員是否存在
        Optional<MemberEntity> memberOpt = memberRepository.findByEmail(email);
        MemberEntity member;

        if (memberOpt.isPresent()) {
            // 會員已存在 -> 直接登入
            member = memberOpt.get();
            // (可選) 您可以在這裡更新使用者的頭像或名字，如果需要同步 Google 資料的話
        } else {
            // 會員不存在 -> 自動註冊
            // 生成一個隨機密碼 (因為是 Google 登入，使用者不需要知道這個密碼)
            String randomPassword = UUID.randomUUID().toString();
            member = createMember(email, randomPassword, name, pictureUrl);
        }

        // 3. 發放我們系統的 Token (預設給長效期，方便使用者)
        return generateAuthResponse(member);
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
                member.getGender(),
                member.getBirthDate(),
                member.getPhone(),
                member.getAddress(),
                member.getCreatedAt()
        );
    }

    @Transactional
    @Override
    public AuthResponse updateProfile(String email, UpdateProfileRequest request) {
        MemberEntity member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("找不到會員資料"));

        // 更新頭像
        if (request.pictureUrl() != null && !request.pictureUrl().isBlank()) {
            String oldUrl = member.getPictureUrl();

            if (oldUrl != null && !isDefaultAvatar(oldUrl)) {
                try {
                    String fileName = oldUrl.substring(oldUrl.lastIndexOf("/") + 1);
                    fileService.deleteFile(fileName);
                } catch (Exception _) {
                }
            }

            // 設定新圖片
            member.setPictureUrl(request.pictureUrl());
        }

        // 更新暱稱
        if (request.displayName() != null && !request.displayName().isEmpty()) {
            member.setDisplayName(request.displayName());

            if (isDefaultAvatar(member.getPictureUrl())) {
                member.setPictureUrl(generateDefaultAvatarUrl(request.displayName()));
            }
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

//        更新性別
        if (request.gender() != null && !request.gender().isBlank()) {
            member.setGender(request.gender());
        }

//        更新出生日期
        if (request.birthDate() != null) {
            member.setBirthDate(request.birthDate());
        }

//        更新手機號碼
        if (request.phone() != null && !request.phone().isBlank()) {
            member.setPhone(request.phone());
        }

//        更新住址
        if (request.address() != null && !request.address().isBlank()) {
            member.setAddress(request.address());
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
                member.getGender(),
                member.getBirthDate(),
                member.getPhone(),
                member.getAddress(),
                member.getCreatedAt()
        );
    }

    // 產生預設頭像網址
    private String generateDefaultAvatarUrl(String seed) {
        return DEFAULT_AVATAR_PREFIX + "/7.x/" + DEFAULT_AVATAR_STYLE + "/svg?seed=" + seed;
    }

    // 判斷是否為預設頭像
    private boolean isDefaultAvatar(String url) {
        if (url == null) return false;
        return url.contains(DEFAULT_AVATAR_PREFIX) && url.contains(DEFAULT_AVATAR_STYLE);
    }

    private MemberEntity createMember(String email, String password, String displayName, String pictureUrl) {
        MemberEntity member = new MemberEntity();
        member.setEmail(email);
        member.setPasswordHash(passwordEncoder.encode(password));

        if (displayName == null || displayName.isBlank()) {
            displayName = email.split("@")[0];
        }
        member.setDisplayName(displayName);

        if (pictureUrl != null && !pictureUrl.isBlank()) {
            member.setPictureUrl(pictureUrl);
        } else {
            member.setPictureUrl(generateDefaultAvatarUrl(displayName));
        }

        RoleEntity userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new RoleEntity("ROLE_USER")));
        member.addRole(userRole);

        if (ADMIN_EMAIL.equalsIgnoreCase(email)) {
            RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(new RoleEntity("ROLE_ADMIN")));
            member.addRole(adminRole);
        }

        return memberRepository.save(member);
    }

    // Google Token 驗證邏輯
    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                return idToken.getPayload();
            } else {
                throw new IllegalArgumentException("Google Token 無效");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Google 登入失敗: " + e.getMessage());
        }
    }

    private AuthResponse generateAuthResponse(MemberEntity member) {
        String roleNames = getRoleNames(member);
        String token = jwtUtil.generateToken(member.getEmail(), roleNames, true);
        return createAuthResponse(member, token, roleNames);
    }

    private String getRoleNames(MemberEntity member) {
        return member.getRoles().stream()
                .map(RoleEntity::getName).collect(Collectors.joining(","));
    }

    private AuthResponse createAuthResponse(MemberEntity member, String token, String roleNames) {
        return new AuthResponse(
                token,
                member.getEmail(),
                member.getDisplayName(),
                member.getPictureUrl(),
                roleNames,
                member.getGender(),
                member.getBirthDate(),
                member.getPhone(),
                member.getAddress(),
                member.getCreatedAt()
        );
    }
}