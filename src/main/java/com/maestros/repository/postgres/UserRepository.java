package com.maestros.repository.postgres;

import com.maestros.base.BaseRepository;
import com.maestros.model.postgres.User;

import java.util.Optional;

public interface UserRepository extends BaseRepository<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
