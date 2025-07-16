package com.adrian.blogweb1.service;


import com.adrian.blogweb1.model.UserSec;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IUserService {

    public List<UserSec> findAll();

    public Optional<UserSec> findById(Long id);

    public UserSec save(UserSec userSec);

    UserSec updateUser(Long idUserSec, UserSec userSec);

    public void deleteUser(Long idUserSec);

    Optional<UserSec> findByEmail(String email);

    UserSec findOrCreateUserForOAuth(String email, String username);

    String encryptPassword(String password);
}

