package com.example.cinema.modules.identity.data;

import com.example.cinema.modules.identity.business.User;
import com.example.cinema.modules.identity.business.UserRepository;
import com.example.cinema.modules.identity.business.UsernameAlreadyExistsException;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
class UserRepositoryAdapter implements UserRepository {
    private final SpringDataUserRepository jpaRepository;

    UserRepositoryAdapter(SpringDataUserRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        try {
            return jpaRepository.save(new UserEntity(user)).toModel();
        } catch (DataIntegrityViolationException exception) {
            if (isUsernameUniqueViolation(exception)) {
                throw new UsernameAlreadyExistsException();
            }
            throw exception;
        }
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(UserEntity::toModel);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(UserEntity::toModel);
    }

    private static boolean isUsernameUniqueViolation(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && "uq_users_username".equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
