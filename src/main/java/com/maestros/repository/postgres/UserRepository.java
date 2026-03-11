package com.maestros.repository.postgres;

import com.maestros.base.BaseRepository;
import com.maestros.model.postgres.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends BaseRepository<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.fcmToken = null WHERE u.fcmToken = :token")
    void clearFcmTokenByValue(@Param("token") String token);
}
