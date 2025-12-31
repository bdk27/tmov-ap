package com.brian.tmov.dao.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "member")
@Data
public class MemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email; // 登入帳號

    @Column(name = "display_name")
    private String displayName; // 顯示名稱

    @Column(name = "picture_url")
    private String pictureUrl; // 大頭貼

    @Column(name = "password_hash", nullable = false)
    private String passwordHash; // 密碼

    @Column(name = "gender")
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    // 多對多關聯：member_has_role
    @ManyToMany(fetch = FetchType.EAGER) // 登入時通常需要立即讀取角色
    @JoinTable(
            name = "member_has_role",
            joinColumns = @JoinColumn(name = "member_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addRole(RoleEntity role) {
        this.roles.add(role);
    }
}
