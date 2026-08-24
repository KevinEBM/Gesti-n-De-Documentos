import {
    extraerMetadatosDesdeArchivo,
    nombresCoinciden,
    type MetadatosExtraidosDocumento,
} from "./extraer-info-doc";
import type { SubprogramaCatalogo } from "./subprogramas-api";
import type { TipoDocumentoCatalogo } from "./tipos-documento-api";

export interface ActualizacionMetadatosPublicacion {
    codigo?: string;
    titulo?: string;
    areaId?: string;
    subprogramaId?: string;
    tipoDocumentoId?: string;
    numeroVersionInicial?: string;
}

export interface ResultadoMetadatosPublicacion {
    actualizaciones: ActualizacionMetadatosPublicacion;
    avisos: string[];
    detectado: boolean;
}

export interface OpcionesResolverMetadatosPublicacion {
    omitirResolucionCatalogo?: boolean;
}

export function resolverMetadatosPublicacion(
    extraccion: MetadatosExtraidosDocumento,
    subprogramas: SubprogramaCatalogo[],
    tipos: TipoDocumentoCatalogo[],
    opciones?: OpcionesResolverMetadatosPublicacion,
): ResultadoMetadatosPublicacion {
    if (!extraccion.reconocido || !extraccion.codigo) {
        return {
            actualizaciones: {},
            avisos: [
                "No se pudo detectar la nomenclatura institucional. Complete o revise los metadatos manualmente.",
            ],
            detectado: false,
        };
    }

    const actualizaciones: ActualizacionMetadatosPublicacion = {
        codigo: extraccion.codigo,
    };
    const avisos: string[] = [];

    if (extraccion.nombre) {
        actualizaciones.titulo = extraccion.nombre;
    }

    if (!opciones?.omitirResolucionCatalogo) {
        resolverCamposCatalogo(extraccion, subprogramas, tipos, actualizaciones, avisos);
    }

    if (extraccion.version.tipo === "entera") {
        actualizaciones.numeroVersionInicial = String(extraccion.version.valor);
    } else if (extraccion.version.tipo === "decimal") {
        avisos.push(
            "Se detectó una versión no compatible con el formato actual. Revise la versión inicial manualmente.",
        );
    }

    return {
        actualizaciones,
        avisos,
        detectado: true,
    };
}

export function aplicarMetadatosDesdeArchivo(
    nombreArchivo: string,
    subprogramas: SubprogramaCatalogo[],
    tipos: TipoDocumentoCatalogo[],
    opciones?: OpcionesResolverMetadatosPublicacion,
): ResultadoMetadatosPublicacion {
    const extraccion = extraerMetadatosDesdeArchivo(nombreArchivo);
    return resolverMetadatosPublicacion(extraccion, subprogramas, tipos, opciones);
}

export function resolverMetadatosCatalogoDesdeArchivo(
    nombreArchivo: string,
    subprogramas: SubprogramaCatalogo[],
    tipos: TipoDocumentoCatalogo[],
): ResultadoMetadatosPublicacion {
    const extraccion = extraerMetadatosDesdeArchivo(nombreArchivo);
    if (!extraccion.reconocido || !extraccion.codigo) {
        return {
            actualizaciones: {},
            avisos: [],
            detectado: false,
        };
    }

    const actualizaciones: ActualizacionMetadatosPublicacion = {};
    const avisos: string[] = [];
    resolverCamposCatalogo(extraccion, subprogramas, tipos, actualizaciones, avisos);

    return {
        actualizaciones,
        avisos,
        detectado: Object.keys(actualizaciones).length > 0,
    };
}

function resolverCamposCatalogo(
    extraccion: MetadatosExtraidosDocumento,
    subprogramas: SubprogramaCatalogo[],
    tipos: TipoDocumentoCatalogo[],
    actualizaciones: ActualizacionMetadatosPublicacion,
    avisos: string[],
): void {
    if (extraccion.nombreSubproceso) {
        const subprograma = buscarSubprogramaActivo(extraccion.nombreSubproceso, subprogramas);
        if (subprograma) {
            actualizaciones.areaId = subprograma.areaId;
            actualizaciones.subprogramaId = subprograma.id;
        } else if (existeSubprogramaInactivo(extraccion.nombreSubproceso, subprogramas)) {
            avisos.push(
                "Se reconoció el subproceso, pero no se encontró en los catálogos activos. Revise los metadatos manualmente.",
            );
        } else {
            avisos.push(
                "Se reconoció el subproceso, pero no se encontró en los catálogos activos. Revise los metadatos manualmente.",
            );
        }
    } else if (extraccion.abreviaturaSubproceso) {
        avisos.push(
            "Se reconoció el subproceso, pero no se encontró en los catálogos activos. Revise los metadatos manualmente.",
        );
    }

    if (extraccion.nombreTipo) {
        const tipo = buscarTipoActivo(extraccion.nombreTipo, tipos);
        if (tipo) {
            actualizaciones.tipoDocumentoId = tipo.id;
        } else if (existeTipoInactivo(extraccion.nombreTipo, tipos)) {
            avisos.push(
                "Se reconoció el tipo de documento, pero no se encontró en los catálogos activos. Revise los metadatos manualmente.",
            );
        } else {
            avisos.push(
                "Se reconoció el tipo de documento, pero no se encontró en los catálogos activos. Revise los metadatos manualmente.",
            );
        }
    } else if (extraccion.abreviaturaTipo) {
        avisos.push(
            "Se reconoció el tipo de documento, pero no se encontró en los catálogos activos. Revise los metadatos manualmente.",
        );
    }
}

function buscarSubprogramaActivo(
    nombreSubproceso: string,
    subprogramas: SubprogramaCatalogo[],
): SubprogramaCatalogo | null {
    return (
        subprogramas.find(
            (item) => item.activo && nombresCoinciden(item.nombre, nombreSubproceso),
        ) ?? null
    );
}

function existeSubprogramaInactivo(
    nombreSubproceso: string,
    subprogramas: SubprogramaCatalogo[],
): boolean {
    return subprogramas.some(
        (item) => !item.activo && nombresCoinciden(item.nombre, nombreSubproceso),
    );
}

function buscarTipoActivo(
    nombreTipo: string,
    tipos: TipoDocumentoCatalogo[],
): TipoDocumentoCatalogo | null {
    return tipos.find((item) => item.activo && nombresCoinciden(item.nombre, nombreTipo)) ?? null;
}

function existeTipoInactivo(
    nombreTipo: string,
    tipos: TipoDocumentoCatalogo[],
): boolean {
    return tipos.some((item) => !item.activo && nombresCoinciden(item.nombre, nombreTipo));
}
