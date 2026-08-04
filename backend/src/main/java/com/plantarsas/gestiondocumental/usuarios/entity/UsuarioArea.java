package com.plantarsas.gestiondocumental.usuarios.entity;

import com.plantarsas.gestiondocumental.areas.entity.Area;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario_area")
@Getter
@NoArgsConstructor
public class UsuarioArea {

    @EmbeddedId
    private UsuarioAreaId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("areaId")
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal = false;

    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private LocalDateTime fechaAsignacion;

    public UsuarioArea(Usuario usuario, Area area, boolean esPrincipal) {
        this.usuario = usuario;
        this.area = area;
        this.esPrincipal = esPrincipal;
        this.id = new UsuarioAreaId(usuario.getId(), area.getId());
    }

    public void cambiarPrincipal(boolean principal) {
        this.esPrincipal = principal;
    }

    @PrePersist
    protected void alCrear() {
        if (fechaAsignacion == null) {
            fechaAsignacion = LocalDateTime.now();
        }
    }
}
