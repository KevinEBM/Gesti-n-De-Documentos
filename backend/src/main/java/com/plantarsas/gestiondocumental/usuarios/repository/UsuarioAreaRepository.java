package com.plantarsas.gestiondocumental.usuarios.repository;

import com.plantarsas.gestiondocumental.usuarios.entity.UsuarioArea;
import com.plantarsas.gestiondocumental.usuarios.entity.UsuarioAreaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsuarioAreaRepository extends JpaRepository<UsuarioArea, UsuarioAreaId> {

    List<UsuarioArea> findByUsuario_Id(Long usuarioId);

    @Modifying(flushAutomatically = true)
    @Query("delete from UsuarioArea ua where ua.usuario.id = :usuarioId")
    int deleteByUsuarioId(@Param("usuarioId") Long usuarioId);
}
