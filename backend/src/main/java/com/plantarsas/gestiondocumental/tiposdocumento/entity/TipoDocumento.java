package com.plantarsas.gestiondocumental.tiposdocumento.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "tipos_documento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TipoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    public TipoDocumento(String codigo, String nombre, String descripcion) {
        this.codigo = normalizarCodigo(codigo);
        this.nombre = normalizarTexto(nombre);
        this.descripcion = normalizarTextoOpcional(descripcion);
        this.activo = true;
    }

    public void actualizarDatos(String codigo, String nombre, String descripcion) {
        this.codigo = normalizarCodigo(codigo);
        this.nombre = normalizarTexto(nombre);
        this.descripcion = normalizarTextoOpcional(descripcion);
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    /**
     * El código puede repetirse entre tipos distintos (p. ej. ODE). La unicidad
     * del registro sigue siendo el id. Misma normalización que {@link com.plantarsas.gestiondocumental.subprogramas.entity.Subprograma}.
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
