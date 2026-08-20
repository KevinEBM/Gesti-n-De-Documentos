package com.plantarsas.gestiondocumental.documentos.entity;

import com.plantarsas.gestiondocumental.shared.enums.DocumentoAlcance;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;
import com.plantarsas.gestiondocumental.subprogramas.entity.Subprograma;
import com.plantarsas.gestiondocumental.tiposdocumento.entity.TipoDocumento;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "documentos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subprograma_id", nullable = false)
    private Subprograma subprograma;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_documento_id", nullable = false)
    private TipoDocumento tipoDocumento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por", nullable = false)
    private Usuario creadoPor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentoEstado estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "alcance", nullable = false, length = 20)
    private DocumentoAlcance alcance;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    public Documento(
            String codigo,
            String titulo,
            String descripcion,
            Subprograma subprograma,
            TipoDocumento tipoDocumento,
            Usuario creadoPor
    ) {
        this(codigo, titulo, descripcion, subprograma, tipoDocumento, creadoPor, DocumentoAlcance.AREA_RESPONSABLE);
    }

    public Documento(
            String codigo,
            String titulo,
            String descripcion,
            Subprograma subprograma,
            TipoDocumento tipoDocumento,
            Usuario creadoPor,
            DocumentoAlcance alcance
    ) {
        this.codigo = normalizarTexto(codigo);
        this.titulo = normalizarTexto(titulo);
        this.descripcion = normalizarTextoOpcional(descripcion);
        this.subprograma = subprograma;
        this.tipoDocumento = tipoDocumento;
        this.creadoPor = creadoPor;
        this.estado = DocumentoEstado.PUBLICADO;
        this.alcance = alcance;
    }

    public void cambiarEstado(DocumentoEstado nuevoEstado) {
        this.estado = nuevoEstado;
    }

    public void actualizarMetadatos(
            String codigo,
            String titulo,
            String descripcion,
            Subprograma subprograma,
            TipoDocumento tipoDocumento,
            DocumentoAlcance alcance
    ) {
        this.codigo = normalizarTexto(codigo);
        this.titulo = normalizarTexto(titulo);
        this.descripcion = normalizarTextoOpcional(descripcion);
        this.subprograma = subprograma;
        this.tipoDocumento = tipoDocumento;
        this.alcance = alcance;
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
