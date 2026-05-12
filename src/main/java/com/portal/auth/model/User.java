package com.portal.auth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_wechat_openid", columnList = "wechat_openid"),
        @Index(name = "idx_wechat_unionid", columnList = "wechat_unionid"),
        @Index(name = "idx_wechat_openid_qr", columnList = "wechat_openid_qr")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String phone;

    @Column(nullable = false, unique = true)
    private String username;

    @Column
    private String passwordHash;

    @Column(nullable = false)
    private int tokenVersion;

    @Column(unique = true)
    private String wechatOpenid;

    @Column(unique = true)
    private String wechatUnionid;

    @Column(unique = true)
    private String wechatOpenidQr;

    private String wechatNickname;

    private String wechatAvatar;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public int getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(int tokenVersion) { this.tokenVersion = tokenVersion; }

    public String getWechatOpenid() { return wechatOpenid; }
    public void setWechatOpenid(String wechatOpenid) { this.wechatOpenid = wechatOpenid; }

    public String getWechatUnionid() { return wechatUnionid; }
    public void setWechatUnionid(String wechatUnionid) { this.wechatUnionid = wechatUnionid; }

    public String getWechatOpenidQr() { return wechatOpenidQr; }
    public void setWechatOpenidQr(String wechatOpenidQr) { this.wechatOpenidQr = wechatOpenidQr; }

    public String getWechatNickname() { return wechatNickname; }
    public void setWechatNickname(String wechatNickname) { this.wechatNickname = wechatNickname; }

    public String getWechatAvatar() { return wechatAvatar; }
    public void setWechatAvatar(String wechatAvatar) { this.wechatAvatar = wechatAvatar; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
