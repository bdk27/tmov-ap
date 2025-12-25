package com.brian.tmov.service.impl;

import com.brian.tmov.dao.entity.MemberEntity;
import com.brian.tmov.dao.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
public class UserDetailServiceImpl implements UserDetailsService {

    @Autowired
    private MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true) // 確保能讀取 Lazy Loading 的關聯資料
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 使用 email 查找會員
        MemberEntity member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("找不到會員: " + email));

        // 將 Member 轉換為 Spring Security 的 UserDetails
        return new org.springframework.security.core.userdetails.User(
                member.getEmail(),
                member.getPasswordHash(), // 資料庫中的 password_hash
                member.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList())
        );
    }
}
