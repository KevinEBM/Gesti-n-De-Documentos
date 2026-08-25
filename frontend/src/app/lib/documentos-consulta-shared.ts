import type { DocumentoAlcance, DocumentoEstado, DocumentoFiltros } from "@/lib/documentos-api";

export const etiquetasAlcance: Record<DocumentoAlcance, string> = {
    AREA_RESPONSABLE: "Área responsable",
    AREAS_ESPECIFICAS: "Áreas específicas",
    GLOBAL: "Global",
};

export const TODOS = "todos";
export const DOCUMENTOS_PAGE_SIZE = 10;

/** @deprecated Usar DOCUMENTOS_PAGE_SIZE */
export const TAMANO_PAGINA = DOCUMENTOS_PAGE_SIZE;

export const SELECT_CONTENT_CLASS =
    "!bg-white !text-slate-900 border border-slate-200 shadow-2xl z-[99999]";

export const SELECT_TRIGGER_CLASS = "w-full !bg-white !text-slate-900";

export const SELECT_ITEM_CLASS =
    "!text-slate-900 focus:!bg-slate-100 focus:!text-slate-900 data-[highlighted]:!bg-slate-100 data-[highlighted]:!text-slate-900";

export interface FiltrosDocumentos {
    codigo: string;
    titulo: string;
    area: string;
    subprograma: string;
    tipo: string;
    estado: string;
    soloGlobales: boolean;
    fechaDesde: string;
    fechaHasta: string;
}

export const filtrosVacios: FiltrosDocumentos = {
    codigo: "",
    titulo: "",
    area: TODOS,
    subprograma: TODOS,
    tipo: TODOS,
    estado: TODOS,
    soloGlobales: false,
    fechaDesde: "",
    fechaHasta: "",
};

export interface ConstruirFiltrosApiOpciones {
    /** Solo ADMIN debe enviar areaId como filtro de consulta. No-admin: visiblePara en backend. */
    incluirAreaEnConsulta?: boolean;
}

export interface HayFiltrosActivosOpciones {
    /** Área preseleccionada/obligatoria que no cuenta como filtro del usuario. */
    areaNoCuentaComoFiltro?: string;
}

export const etiquetasEstado: Record<DocumentoEstado, string> = {
    PUBLICADO: "Publicado",
    INACTIVO: "Inactivo",
    OBSOLETO: "Obsoleto",
};

export const estilosEstado: Record<DocumentoEstado, string> = {
    PUBLICADO: "border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400",
    INACTIVO: "border-zinc-500/30 bg-zinc-500/10 text-zinc-600 dark:text-zinc-400",
    OBSOLETO: "border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400",
};

/** Selector de estado en Gestión: tema claro forzado, sin depender del modo oscuro global. */
export const estilosEstadoAdminSelect: Record<DocumentoEstado, string> = {
    PUBLICADO: "!bg-emerald-50 !text-emerald-800 border-emerald-300 hover:!bg-emerald-100",
    INACTIVO: "!bg-red-50 !text-red-800 border-red-300 hover:!bg-red-100",
    OBSOLETO: "!bg-amber-50 !text-amber-800 border-amber-300 hover:!bg-amber-100",
};

export const ALERT_DIALOG_CONTENT_CLASS =
    "!bg-white !text-slate-900 border border-slate-200 shadow-2xl";

export const ALERT_DIALOG_TITLE_CLASS = "!text-slate-900";

export const ALERT_DIALOG_TITLE_DESACTIVAR_CLASS = "!text-red-600";

export const ALERT_DIALOG_DESCRIPTION_CLASS = "!text-slate-600";

export const ALERT_DIALOG_CANCEL_CLASS =
    "!bg-white !text-slate-900 border border-slate-200 hover:!bg-slate-100";

/** Mismo verde primario que Button default (#289248). */
export const ALERT_DIALOG_ACTION_CLASS =
    "!bg-[#289248] !text-white hover:!bg-[#289248]/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#289248]/40 disabled:opacity-50";

export type GeneroEtiquetaCatalogo = "femenino" | "masculino";

export function etiquetaCatalogoConsulta(
    nombre: string,
    activo: boolean,
    genero: GeneroEtiquetaCatalogo = "masculino",
): string {
    if (activo) return nombre;
    return genero === "femenino" ? `${nombre} (Inactiva)` : `${nombre} (Inactivo)`;
}

export function formatFechaDocumento(fecha: string): string {
    try {
        return new Intl.DateTimeFormat("es-CO", {
            dateStyle: "medium",
            timeStyle: "short",
        }).format(new Date(fecha));
    } catch {
        return fecha;
    }
}

export function hayFiltrosActivos(
    filtros: FiltrosDocumentos,
    opciones?: HayFiltrosActivosOpciones,
): boolean {
    const areaEsFiltroUsuario =
        filtros.area !== TODOS && filtros.area !== opciones?.areaNoCuentaComoFiltro;

    return (
        !!filtros.codigo.trim() ||
        !!filtros.titulo.trim() ||
        areaEsFiltroUsuario ||
        filtros.subprograma !== TODOS ||
        filtros.tipo !== TODOS ||
        filtros.estado !== TODOS ||
        filtros.soloGlobales ||
        !!filtros.fechaDesde ||
        !!filtros.fechaHasta
    );
}

export interface AreaCatalogoConsulta {
    id: string;
    nombre: string;
    activo?: boolean;
}

export interface SubprogramaCatalogoConsulta {
    id: string;
    areaId: string;
    nombre?: string;
    activo?: boolean;
}

export interface ResolverAreaAsignadaOpciones {
    /** Áreas devueltas por GET /api/areas (solo asignaciones autorizadas/principal en BD). */
    areasUsuario: AreaCatalogoConsulta[];
    /** true tras respuesta exitosa de GET /api/areas; false mientras carga o si falló. */
    areasApiCargadas: boolean;
    /** Snapshot del login; solo fallback temporal antes de que la API responda. */
    sesionAreaId?: string;
    sesionAreaNombre?: string | null;
}

export interface AreaAsignadaConsulta {
    areaId: string;
    nombre: string;
}

/**
 * Resuelve el área principal asignada de un no-admin.
 *
 * Contrato GET /api/areas para JEFE_AREA/ADMINISTRATIVO:
 * devuelve las áreas de {@code obtenerAreaIdsAutorizadas} (solo principal/es).
 * En operación normal hay 0 o 1 elemento; si hay más de uno, no se resuelve.
 */
export function resolverAreaAsignadaNoAdmin(
    opciones: ResolverAreaAsignadaOpciones,
): AreaAsignadaConsulta | null {
    const { areasUsuario, areasApiCargadas, sesionAreaId, sesionAreaNombre } = opciones;

    if (areasApiCargadas) {
        if (areasUsuario.length === 1) {
            return {
                areaId: areasUsuario[0].id,
                nombre: areasUsuario[0].nombre,
            };
        }
        return null;
    }

    if (sesionAreaId) {
        const nombre = sesionAreaNombre?.trim();
        if (nombre) {
            return { areaId: sesionAreaId, nombre };
        }
        return { areaId: sesionAreaId, nombre: sesionAreaId };
    }

    return null;
}

export interface ResolverAreaIdEfectivaOpciones {
    esAdmin: boolean;
    filtroArea: string;
    areasUsuario: AreaCatalogoConsulta[];
    areasApiCargadas: boolean;
    sesionAreaId?: string;
    sesionAreaNombre?: string | null;
}

/** Área usada para filtrar subprocesos en formulario de consulta. */
export function resolverAreaIdEfectivaConsulta(
    opciones: ResolverAreaIdEfectivaOpciones,
): string {
    const {
        esAdmin,
        filtroArea,
        areasUsuario,
        areasApiCargadas,
        sesionAreaId,
        sesionAreaNombre,
    } = opciones;

    if (esAdmin) {
        return filtroArea !== TODOS ? filtroArea : TODOS;
    }

    const asignada = resolverAreaAsignadaNoAdmin({
        areasUsuario,
        areasApiCargadas,
        sesionAreaId,
        sesionAreaNombre,
    });
    if (asignada) {
        return asignada.areaId;
    }

    return TODOS;
}

export function resolverAreaObligatoriaNoAdmin(
    esAdmin: boolean,
    areasUsuario: AreaCatalogoConsulta[],
    areasApiCargadas: boolean,
    sesionAreaId?: string,
    sesionAreaNombre?: string | null,
): string | undefined {
    if (esAdmin) {
        return undefined;
    }
    return resolverAreaAsignadaNoAdmin({
        areasUsuario,
        areasApiCargadas,
        sesionAreaId,
        sesionAreaNombre,
    })?.areaId;
}

export function construirFiltrosBaseConsulta(
    esAdmin: boolean,
    areasUsuario: AreaCatalogoConsulta[],
    areasApiCargadas: boolean,
    sesionAreaId?: string,
    sesionAreaNombre?: string | null,
): FiltrosDocumentos {
    const areaObligatoria = resolverAreaObligatoriaNoAdmin(
        esAdmin,
        areasUsuario,
        areasApiCargadas,
        sesionAreaId,
        sesionAreaNombre,
    );
    if (areaObligatoria) {
        return { ...filtrosVacios, area: areaObligatoria };
    }
    return filtrosVacios;
}

/** El selector de Área queda fijo para no-admin (con o sin asignación resuelta). */
export function areaConsultaNoAdminBloqueada(esAdmin: boolean): boolean {
    return !esAdmin;
}

export function resolverAreaPrincipalDesdeCatalogo(
    areas: AreaCatalogoConsulta[],
    areasApiCargadas: boolean,
): { areaId: string; areaPrincipalNombre: string } | null {
    const asignada = resolverAreaAsignadaNoAdmin({
        areasUsuario: areas,
        areasApiCargadas,
    });
    if (!asignada) {
        return null;
    }
    return {
        areaId: asignada.areaId,
        areaPrincipalNombre: asignada.nombre,
    };
}

export function filtrarSubprogramasConsulta(
    subprogramas: SubprogramaCatalogoConsulta[],
    opciones: { esAdmin: boolean; areaIdEfectiva: string },
): SubprogramaCatalogoConsulta[] {
    if (opciones.esAdmin && opciones.areaIdEfectiva === TODOS) {
        return subprogramas;
    }
    if (opciones.areaIdEfectiva === TODOS) {
        return [];
    }
    return subprogramas.filter((item) => item.areaId === opciones.areaIdEfectiva);
}

export function subprocesoConsultaDeshabilitado(
    esAdmin: boolean,
    areaIdEfectiva: string,
    cargandoCatalogos: boolean,
    errorCatalogos: string | null | undefined,
): boolean {
    if (cargandoCatalogos || !!errorCatalogos) {
        return true;
    }
    if (esAdmin) {
        return false;
    }
    return areaIdEfectiva === TODOS;
}

export function construirFiltrosApi(
    filtros: FiltrosDocumentos,
    page: number,
    opciones?: ConstruirFiltrosApiOpciones,
): DocumentoFiltros {
    const incluirArea = opciones?.incluirAreaEnConsulta ?? true;
    const api: DocumentoFiltros = { page, size: DOCUMENTOS_PAGE_SIZE };

    const codigo = filtros.codigo.trim();
    const titulo = filtros.titulo.trim();

    if (codigo) api.codigo = codigo;
    if (titulo) api.titulo = titulo;
    if (incluirArea && filtros.area !== TODOS) api.areaId = Number(filtros.area);
    if (filtros.subprograma !== TODOS) api.subprogramaId = Number(filtros.subprograma);
    if (filtros.tipo !== TODOS) api.tipoDocumentoId = Number(filtros.tipo);
    if (filtros.estado !== TODOS) api.estado = filtros.estado as DocumentoEstado;
    if (filtros.fechaDesde) api.fechaDesde = filtros.fechaDesde;
    if (filtros.fechaHasta) api.fechaHasta = filtros.fechaHasta;
    if (filtros.soloGlobales) {
        api.alcanceConsulta = "GLOBALES";
    }

    return api;
}

export function dispararDescargaEnNavegador(blob: Blob, nombreArchivo: string) {
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement("a");
    enlace.href = url;
    enlace.download = nombreArchivo;
    enlace.click();
    URL.revokeObjectURL(url);
}
