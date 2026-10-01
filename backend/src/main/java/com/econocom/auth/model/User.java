package com.econocom.auth.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entidad JPA que representa un usuario de la aplicación.
 * <p>
 * Mapeada a la tabla {@code users}. Se guarda el <b>hash</b> BCrypt de la contraseña,
 * nunca la contraseña en texto plano.
 * </p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /** Correo electrónico. Se usa como clave primaria y credencial de login. */
    @Id
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** Hash BCrypt de la contraseña. */
    @Column(nullable = false, length = 100)
    private String passwordHash;

    /** Nombre visible del usuario. */
    @Column(nullable = false, length = 100)
    private String name;
}
