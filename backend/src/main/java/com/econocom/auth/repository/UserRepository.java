package com.econocom.auth.repository;

import com.econocom.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para {@link User} utilizando Spring Data JPA.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Busca un usuario por su correo electrónico ignorando mayúsculas/minúsculas.
     *
     * @param email correo del usuario
     * @return el usuario si existe, o {@link Optional#empty()} si no
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Adaptador para mantener compatibilidad con el método original de búsqueda.
     *
     * @param email correo del usuario
     * @return el usuario si existe, o {@link Optional#empty()} si no
     */
    default Optional<User> findByEmail(String email) {
        return (email == null) ? Optional.empty() : findByEmailIgnoreCase(email.trim());
    }
}
