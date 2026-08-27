import {
    extraerMetadatosDesdeArchivo,
    nombresCoinciden,
    catalogoTiposTieneCodigos,
    coincidenciasTipoPorCodigo,
    resolverTipoUnicoPorCodigo,
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

/**
 * Códigos usados en nombres de archivo antiguos, anteriores a que el símbolo
 * "&" formara parte del código institucional. Solo se aplican cuando el código
 * leído no coincide con ningún subprograma del catálogo.
 */
const ALIASES_SUBPROCESO: Record<string, string> = {
    CD: "C&D",
    LD: "L&D",
};

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
    if (extraccion.codigoSubproceso) {
        const subprograma = buscarSubprogramaPorCodigo(
            extraccion.codigoSubproceso,
            subprogramas,
        );
        if (subprograma?.activo) {
            actualizaciones.areaId = subprograma.areaId;
            actualizaciones.subprogramaId = subprograma.id;
        } else {
            avisos.push(
                "Se reconoció el subproceso, pero no se encontró en los catálogos activos. Revise los metadatos manualmente.",
            );
        }
    }

    if (extraccion.abreviaturaTipo) {
        const tipoUnico = resolverTipoUnicoPorCodigo(extraccion.abreviaturaTipo, tipos);
        if (tipoUnico) {
            actualizaciones.tipoDocumentoId = tipoUnico.id;
            return;
        }

        const coincidencias = coincidenciasTipoPorCodigo(extraccion.abreviaturaTipo, tipos);
        if (coincidencias.length > 1) {
            avisos.push(
                "El código de tipo coincide con varios registros. Seleccione el tipo de documento manualmente.",
            );
            return;
        }

        if (catalogoTiposTieneCodigos(tipos)) {
            avisos.push(
                "Se reconoció el tipo de documento, pero no se encontró en los catálogos activos. Revise los metadatos manualmente.",
            );
            return;
        }
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

/**
 * Coincidencia exacta normalizada contra el código parametrizado en la BD.
 * Si el código leído no existe se reintenta una única vez con su equivalente
 * canónico, para nombres de archivo históricos como PR-LD-PG-01.
 */
function buscarSubprogramaPorCodigo(
    codigoSubproceso: string,
    subprogramas: SubprogramaCatalogo[],
): SubprogramaCatalogo | null {
    const codigo = normalizarCodigo(codigoSubproceso);
    const coincidencia = buscarPorCodigoExacto(codigo, subprogramas);
    if (coincidencia) {
        return coincidencia;
    }

    const canonico = ALIASES_SUBPROCESO[codigo];
    return canonico ? buscarPorCodigoExacto(canonico, subprogramas) : null;
}

function buscarPorCodigoExacto(
    codigo: string,
    subprogramas: SubprogramaCatalogo[],
): SubprogramaCatalogo | null {
    return subprogramas.find((item) => normalizarCodigo(item.codigo) === codigo) ?? null;
}

function normalizarCodigo(codigo: string): string {
    return codigo.trim().toUpperCase();
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
