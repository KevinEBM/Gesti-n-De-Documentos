import { API_BASE_URL, ApiError, type ApiResponse, apiFetch } from "./api";
import { getToken } from "./auth-storage";
import { DOCUMENTOS_PAGE_SIZE } from "./documentos-consulta-shared";

export type DocumentoEstado = "PUBLICADO" | "INACTIVO" | "OBSOLETO";

export type DocumentoAlcance =
    | "AREA_RESPONSABLE"
    | "AREAS_ESPECIFICAS"
    | "GLOBAL";

export interface AreaResumenResponseDto {
    id: number;
    nombre: string;
}

export interface DocumentoPublicacionInicialRequestDto {
    codigo: string;
    titulo: string;
    descripcion?: string | null;
    areaId: number;
    subprogramaId: number;
    tipoDocumentoId: number;
    descripcionVersionInicial: string;
    alcance: DocumentoAlcance;
    areasAdicionalesIds: number[];
}

export interface DocumentoActualizacionRequestDto {
    codigo: string;
    titulo: string;
    descripcion?: string | null;
    areaId: number;
    subprogramaId: number;
    tipoDocumentoId: number;
    alcance: DocumentoAlcance;
    areasAdicionalesIds: number[];
}

export interface DocumentoResumenResponseDto {
    id: number;
    codigo: string;
    titulo: string;
    estado: DocumentoEstado;
    alcance: DocumentoAlcance;
    subprogramaNombre: string;
    tipoDocumentoNombre: string;
    fechaActualizacion: string;
}

export interface DocumentoResponseDto {
    id: number;
    codigo: string;
    titulo: string;
    descripcion: string | null;
    estado: DocumentoEstado;
    areaId: number;
    areaNombre: string;
    subprogramaId: number;
    subprogramaNombre: string;
    tipoDocumentoId: number;
    tipoDocumentoNombre: string;
    creadoPorId: number;
    numeroVersionActual: number;
    nombreArchivoOriginal: string;
    tipoMime: string;
    tamanoBytes: number;
    descripcionVersionActual: string;
    publicadoPorId: number;
    fechaPublicacionVersion: string;
    fechaCreacion: string;
    fechaActualizacion: string;
    alcance: DocumentoAlcance;
    areasAdicionales: AreaResumenResponseDto[];
}

interface PageResponseDto<T> {
    contenido: T[];
    pagina: number;
    tamano: number;
    totalElementos: number;
    totalPaginas: number;
}

export interface AreaResumen {
    id: string;
    nombre: string;
}

export interface DocumentoResumen {
    id: string;
    codigo: string;
    titulo: string;
    estado: DocumentoEstado;
    alcance: DocumentoAlcance;
    subprogramaNombre: string;
    tipoDocumentoNombre: string;
    fechaActualizacion: string;
}

export interface DocumentoDetalle {
    id: string;
    codigo: string;
    titulo: string;
    descripcion: string | null;
    estado: DocumentoEstado;
    areaId: string;
    areaNombre: string;
    subprogramaId: string;
    subprogramaNombre: string;
    tipoDocumentoId: string;
    tipoDocumentoNombre: string;
    creadoPorId: string;
    numeroVersionActual: number;
    nombreArchivoOriginal: string;
    tipoMime: string;
    tamanoBytes: number;
    descripcionVersionActual: string;
    publicadoPorId: string;
    fechaPublicacionVersion: string;
    fechaCreacion: string;
    fechaActualizacion: string;
    alcance: DocumentoAlcance;
    areasAdicionales: AreaResumen[];
}

export interface DocumentoFiltros {
    page?: number;
    size?: number;
    codigo?: string;
    titulo?: string;
    areaId?: number;
    subprogramaId?: number;
    tipoDocumentoId?: number;
    estado?: DocumentoEstado;
    fechaDesde?: string;
    fechaHasta?: string;
}

export interface DocumentosPaginados {
    contenido: DocumentoResumen[];
    pagina: number;
    tamano: number;
    totalElementos: number;
    totalPaginas: number;
}

export interface DocumentoDescarga {
    blob: Blob;
    nombreArchivo: string | null;
    tipoMime: string | null;
}

function mapAreaResumenResponseDto(dto: AreaResumenResponseDto): AreaResumen {
    return {
        id: String(dto.id),
        nombre: dto.nombre,
    };
}

export function mapDocumentoResumenResponseDto(
    dto: DocumentoResumenResponseDto,
): DocumentoResumen {
    return {
        id: String(dto.id),
        codigo: dto.codigo,
        titulo: dto.titulo,
        estado: dto.estado,
        alcance: dto.alcance,
        subprogramaNombre: dto.subprogramaNombre,
        tipoDocumentoNombre: dto.tipoDocumentoNombre,
        fechaActualizacion: dto.fechaActualizacion,
    };
}

export function mapDocumentoResponseDto(dto: DocumentoResponseDto): DocumentoDetalle {
    return {
        id: String(dto.id),
        codigo: dto.codigo,
        titulo: dto.titulo,
        descripcion: dto.descripcion,
        estado: dto.estado,
        areaId: String(dto.areaId),
        areaNombre: dto.areaNombre,
        subprogramaId: String(dto.subprogramaId),
        subprogramaNombre: dto.subprogramaNombre,
        tipoDocumentoId: String(dto.tipoDocumentoId),
        tipoDocumentoNombre: dto.tipoDocumentoNombre,
        creadoPorId: String(dto.creadoPorId),
        numeroVersionActual: dto.numeroVersionActual,
        nombreArchivoOriginal: dto.nombreArchivoOriginal,
        tipoMime: dto.tipoMime,
        tamanoBytes: dto.tamanoBytes,
        descripcionVersionActual: dto.descripcionVersionActual,
        publicadoPorId: String(dto.publicadoPorId),
        fechaPublicacionVersion: dto.fechaPublicacionVersion,
        fechaCreacion: dto.fechaCreacion,
        fechaActualizacion: dto.fechaActualizacion,
        alcance: dto.alcance,
        areasAdicionales: dto.areasAdicionales.map(mapAreaResumenResponseDto),
    };
}

function construirQueryDocumentos(filtros: DocumentoFiltros = {}): string {
    const params = new URLSearchParams();
    const page = filtros.page ?? 0;
    const size = filtros.size ?? DOCUMENTOS_PAGE_SIZE;

    params.set("page", String(page));
    params.set("size", String(size));

    if (filtros.codigo?.trim()) {
        params.set("codigo", filtros.codigo.trim());
    }
    if (filtros.titulo?.trim()) {
        params.set("titulo", filtros.titulo.trim());
    }
    if (filtros.areaId != null) {
        params.set("areaId", String(filtros.areaId));
    }
    if (filtros.subprogramaId != null) {
        params.set("subprogramaId", String(filtros.subprogramaId));
    }
    if (filtros.tipoDocumentoId != null) {
        params.set("tipoDocumentoId", String(filtros.tipoDocumentoId));
    }
    if (filtros.estado) {
        params.set("estado", filtros.estado);
    }
    if (filtros.fechaDesde) {
        params.set("fechaDesde", filtros.fechaDesde);
    }
    if (filtros.fechaHasta) {
        params.set("fechaHasta", filtros.fechaHasta);
    }

    return params.toString();
}

function mapPageResponseDto(
    dto: PageResponseDto<DocumentoResumenResponseDto>,
): DocumentosPaginados {
    return {
        contenido: dto.contenido.map(mapDocumentoResumenResponseDto),
        pagina: dto.pagina,
        tamano: dto.tamano,
        totalElementos: dto.totalElementos,
        totalPaginas: dto.totalPaginas,
    };
}

export async function listarDocumentos(
    filtros: DocumentoFiltros = {},
): Promise<DocumentosPaginados> {
    const query = construirQueryDocumentos(filtros);
    const dto = await apiFetch<PageResponseDto<DocumentoResumenResponseDto>>(
        `/api/documentos?${query}`,
    );
    return mapPageResponseDto(dto);
}

export async function obtenerDocumento(id: string): Promise<DocumentoDetalle> {
    const dto = await apiFetch<DocumentoResponseDto>(`/api/documentos/${id}`);
    return mapDocumentoResponseDto(dto);
}

export async function actualizarDocumento(
    id: string,
    metadata: DocumentoActualizacionRequestDto,
): Promise<DocumentoDetalle> {
    const dto = await apiFetch<DocumentoResponseDto>(`/api/documentos/${id}`, {
        method: "PUT",
        body: JSON.stringify(metadata),
    });
    return mapDocumentoResponseDto(dto);
}

export async function publicarDocumentoInicial(
    metadata: DocumentoPublicacionInicialRequestDto,
    archivo: File,
): Promise<DocumentoDetalle> {
    const formData = new FormData();
    formData.append(
        "metadata",
        new Blob([JSON.stringify(metadata)], { type: "application/json" }),
    );
    formData.append("archivo", archivo);

    const dto = await apiFetch<DocumentoResponseDto>("/api/documentos", {
        method: "POST",
        body: formData,
    });
    return mapDocumentoResponseDto(dto);
}

function extraerNombreArchivo(contentDisposition: string | null): string | null {
    if (!contentDisposition) return null;

    const filenameStar = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
    if (filenameStar?.[1]) {
        try {
            return decodeURIComponent(filenameStar[1].trim());
        } catch {
            return filenameStar[1].trim();
        }
    }

    const filename = contentDisposition.match(/filename="([^"]+)"/i);
    if (filename?.[1]) {
        return filename[1];
    }

    const filenameSinComillas = contentDisposition.match(/filename=([^;]+)/i);
    return filenameSinComillas?.[1]?.trim() ?? null;
}

async function lanzarApiErrorDesdeRespuesta(response: Response): Promise<never> {
    try {
        const payload = (await response.json()) as ApiResponse<unknown>;
        throw new ApiError(
            response.status,
            payload.mensaje ?? `Error HTTP ${response.status}`,
            payload.errores ?? undefined,
        );
    } catch (error) {
        if (error instanceof ApiError) {
            throw error;
        }
        throw new ApiError(response.status, `Error HTTP ${response.status}`);
    }
}

export async function descargarVersionVigente(id: string): Promise<DocumentoDescarga> {
    const headers = new Headers();
    const token = getToken();

    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    const response = await fetch(`${API_BASE_URL}/api/documentos/${id}/descarga`, {
        headers,
    });

    if (!response.ok) {
        await lanzarApiErrorDesdeRespuesta(response);
    }

    const blob = await response.blob();
    const contentDisposition = response.headers.get("Content-Disposition");
    const tipoMime = response.headers.get("Content-Type");

    return {
        blob,
        nombreArchivo: extraerNombreArchivo(contentDisposition),
        tipoMime: tipoMime?.split(";")[0]?.trim() ?? null,
    };
}
