package com.plantarsas.gestiondocumental.documentos.entity;

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "documento_area")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentoArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento_area")
    private Long idDocumentoArea;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal;

    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private LocalDateTime fechaAsignacion;

    public DocumentoArea(Documento documento, Area area) {
        this(documento, area, true);
    }

    public static DocumentoArea principal(Documento documento, Area area) {
        return new DocumentoArea(documento, area, true);
    }

    public static DocumentoArea adicional(Documento documento, Area area) {
        return new DocumentoArea(documento, area, false);
    }

    private DocumentoArea(Documento documento, Area area, boolean esPrincipal) {
        this.documento = documento;
        this.area = area;
        this.esPrincipal = esPrincipal;
    }

    @PrePersist
    protected void alCrear() {
        if (fechaAsignacion == null) {
            fechaAsignacion = LocalDateTime.now();
        }
    }
}
