package com.plantarsas.gestiondocumental.usuarios.entity;

import com.plantarsas.gestiondocumental.roles.entity.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "usuarios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, length = 150)
    private String correo;

    @Getter(AccessLevel.NONE)
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoUsuario estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    public Usuario(String nombres, String apellidos, String correo, String passwordHash, Rol rol) {
        this.nombres = normalizarTexto(nombres);
        this.apellidos = normalizarTexto(apellidos);
        this.correo = normalizarCorreo(correo);
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.estado = EstadoUsuario.ACTIVO;
    }

    public void actualizarDatos(String nombres, String apellidos, String correo) {
        this.nombres = normalizarTexto(nombres);
        this.apellidos = normalizarTexto(apellidos);
        this.correo = normalizarCorreo(correo);
    }

    public void cambiarRol(Rol rol) {
        this.rol = rol;
    }

    public void cambiarEstado(EstadoUsuario estado) {
        this.estado = estado;
    }

    public void actualizarPassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean coincideConPassword(String contrasena, PasswordEncoder passwordEncoder) {
        if (contrasena == null || contrasena.isBlank()) {
            return false;
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        if (passwordEncoder == null) {
            throw new IllegalArgumentException("El codificador de contrasenas es obligatorio");
        }

        return passwordEncoder.matches(contrasena, passwordHash);
    }

    private String normalizarTexto(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String normalizarCorreo(String correo) {
        return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
    }

    @PrePersist
    protected void alCrear() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }
        if (fechaActualizacion == null) {
            fechaActualizacion = ahora;
        }
    }

    @PreUpdate
    protected void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }
}
