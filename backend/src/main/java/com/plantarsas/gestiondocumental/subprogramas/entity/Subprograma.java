package com.plantarsas.gestiondocumental.subprogramas.entity;

import com.plantarsas.gestiondocumental.areas.entity.Area;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "subprogramas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subprograma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    public Subprograma(String codigo, String nombre, String descripcion, Area area) {
        this.codigo = normalizarCodigo(codigo);
        this.nombre = normalizarTexto(nombre);
        this.descripcion = normalizarTextoOpcional(descripcion);
        this.area = area;
        this.activo = true;
    }

    public void actualizarDatos(String codigo, String nombre, String descripcion, Area area) {
        this.codigo = normalizarCodigo(codigo);
        this.nombre = normalizarTexto(nombre);
        this.descripcion = normalizarTextoOpcional(descripcion);
        if (area != null) {
            this.area = area;
        }
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    /**
     * Regla única de normalización del código, compartida con la capa de
     * servicio para que la validación de unicidad compare el mismo valor
     * que finalmente se persiste.
     */
    public static String normalizarCodigo(String codigo) {
        return codigo == null
                ? null
                : codigo.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarTexto(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim();
        return normalizado.isEmpty() ? null : normalizado;
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
