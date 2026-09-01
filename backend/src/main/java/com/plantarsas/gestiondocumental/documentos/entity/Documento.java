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
import java.time.ZoneOffset;

@Entity
@Table(name = "documentos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Documento {

    /** Años completos que un documento debe permanecer OBSOLETO antes de poder eliminarse. */
    public static final int ANOS_RETENCION_OBSOLETO = 2;

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

    /** Instante UTC en que el documento pasó a OBSOLETO; null en cualquier otro estado. */
    @Column(name = "fecha_obsolescencia")
    private LocalDateTime fechaObsolescencia;

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

    /**
     * Aplica la transición de estado manteniendo sincronizado el plazo de retención:
     * entrar en OBSOLETO abre un período nuevo, permanecer en OBSOLETO conserva el
     * original y salir de OBSOLETO lo descarta. La fecha anterior nunca se reutiliza.
     */
    public void cambiarEstado(DocumentoEstado nuevoEstado, LocalDateTime ahoraUtc) {
        if (nuevoEstado != DocumentoEstado.OBSOLETO) {
            this.fechaObsolescencia = null;
        } else if (this.estado != DocumentoEstado.OBSOLETO) {
            this.fechaObsolescencia = ahoraUtc;
        }
        this.estado = nuevoEstado;
    }

    /** Fecha a partir de la cual un documento obsoleto puede eliminarse; null si no aplica. */
    public LocalDateTime fechaDisponibleEliminacion() {
        if (fechaObsolescencia == null) {
            return null;
        }
        return fechaObsolescencia.plusYears(ANOS_RETENCION_OBSOLETO);
    }

    /** Cierto solo si el documento lleva el plazo completo de retención en OBSOLETO. */
    public boolean esAptoParaEliminacion(LocalDateTime ahoraUtc) {
        if (estado != DocumentoEstado.OBSOLETO) {
            return false;
        }
        LocalDateTime disponibleDesde = fechaDisponibleEliminacion();
        return disponibleDesde != null && !ahoraUtc.isBefore(disponibleDesde);
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

    public void registrarFechasUtc(LocalDateTime ahoraUtc) {
        this.fechaCreacion = ahoraUtc;
        this.fechaActualizacion = ahoraUtc;
    }

    public void registrarActualizacionUtc(LocalDateTime ahoraUtc) {
        this.fechaActualizacion = ahoraUtc;
    }

    @PrePersist
    protected void alCrear() {
        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);
        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }
        if (fechaActualizacion == null) {
            fechaActualizacion = ahora;
        }
    }

    @PreUpdate
    protected void alActualizar() {
        if (fechaActualizacion == null) {
            fechaActualizacion = LocalDateTime.now(ZoneOffset.UTC);
        }
    }
}
