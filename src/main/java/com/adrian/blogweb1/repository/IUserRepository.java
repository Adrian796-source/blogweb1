package com.adrian.blogweb1.repository;



import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.model.UserSec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IUserRepository extends JpaRepository <UserSec, Long>{

    //Crea la sentencia en base al nombre en inglés del metodo
    //Tmb se puede hacer mediante Query pero en este caso no es necesario
    Optional<UserSec> findByUsername(String username);

    List<UserSec> findByRolesListContains(Role role);

    Optional<UserSec> findByEmail(String email);


}

