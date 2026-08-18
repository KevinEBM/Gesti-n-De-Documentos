package com.plantarsas.gestiondocumental.usuarios.repository;

import com.plantarsas.gestiondocumental.usuarios.entity.EstadoUsuario;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    long countByEstado(EstadoUsuario estado);
}
