package com.portal.auth.repository;

import com.portal.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByUsername(String username);
    Optional<User> findByWechatOpenid(String wechatOpenid);
    Optional<User> findByWechatUnionid(String wechatUnionid);
    Optional<User> findByWechatOpenidQr(String wechatOpenidQr);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByUsername(String username);
    boolean existsByWechatOpenid(String wechatOpenid);
    boolean existsByWechatOpenidQr(String wechatOpenidQr);
}
