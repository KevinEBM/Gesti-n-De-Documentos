package com.plantarsas.gestiondocumental.documentos.entity;

import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "versiones_documento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VersionDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @Column(name = "numero_version", nullable = false)
    private int numeroVersion;

    @Column(name = "nombre_archivo_original", nullable = false, length = 255)
    private String nombreArchivoOriginal;

    @Column(name = "nombre_archivo_almacenado", nullable = false, length = 255)
    private String nombreArchivoAlmacenado;

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "tipo_mime", nullable = false, length = 150)
    private String tipoMime;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Column(name = "descripcion_cambio", nullable = false, length = 500)
    private String descripcionCambio;

    @Column(name = "fecha_publicacion", nullable = false, updatable = false)
    private LocalDateTime fechaPublicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicado_por", nullable = false)
    private Usuario publicadoPor;

    @Column(nullable = false)
    private boolean vigente;

    public VersionDocumento(
            Documento documento,
            int numeroVersion,
            String nombreArchivoOriginal,
            String nombreArchivoAlmacenado,
            String rutaArchivo,
            String tipoMime,
            long tamanoBytes,
            String descripcionCambio,
            Usuario publicadoPor
    ) {
        this.documento = documento;
        this.numeroVersion = numeroVersion;
        this.nombreArchivoOriginal = nombreArchivoOriginal;
        this.nombreArchivoAlmacenado = nombreArchivoAlmacenado;
        this.rutaArchivo = rutaArchivo;
        this.tipoMime = tipoMime;
        this.tamanoBytes = tamanoBytes;
        this.descripcionCambio = descripcionCambio;
        this.publicadoPor = publicadoPor;
        this.vigente = true;
    }

    public void marcarNoVigente() {
        this.vigente = false;
    }

    public void registrarFechaPublicacionUtc(LocalDateTime ahoraUtc) {
        this.fechaPublicacion = ahoraUtc;
    }

    @PrePersist
    protected void alCrear() {
        if (fechaPublicacion == null) {
            fechaPublicacion = LocalDateTime.now(ZoneOffset.UTC);
        }
    }
}
