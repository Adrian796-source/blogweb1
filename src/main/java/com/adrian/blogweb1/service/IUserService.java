package com.adrian.blogweb1.service;


import com.adrian.blogweb1.model.UserSec;
import java.util.List;
import java.util.Optional;

public interface IUserService {

    List<UserSec> findAll();

    Optional<UserSec> findById(Long id);

    UserSec save(UserSec userSec);

    UserSec updateUser(Long idUserSec, UserSec userSec);

    void deleteUser(Long idUserSec);

    Optional<UserSec> findByEmail(String email);

    UserSec findOrCreateUserForOAuth(String email, String username);

    String encryptPassword(String password);
}

