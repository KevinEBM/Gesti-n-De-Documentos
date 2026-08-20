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
        !!filtros.fechaDesde ||
        !!filtros.fechaHasta
    );
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
