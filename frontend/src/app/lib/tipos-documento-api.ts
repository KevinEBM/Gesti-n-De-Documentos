import { apiFetch } from "./api";

export interface TipoDocumentoResponseDto {
    id: number;
    codigo: string;
    nombre: string;
    descripcion: string | null;
    activo: boolean;
    fechaCreacion: string;
    fechaActualizacion: string;
}

export interface TipoDocumentoRequestDto {
    codigo: string;
    nombre: string;
    descripcion: string;
}

export interface TipoDocumentoUpdateRequestDto {
    codigo: string;
    nombre: string;
    descripcion: string;
}

export interface TipoDocumentoEstadoRequestDto {
    activo: boolean;
}

export interface TipoDocumentoCatalogo {
    id: string;
    codigo: string;
    nombre: string;
    descripcion: string;
    activo: boolean;
}

export function mapTipoDocumentoResponseDto(
    dto: TipoDocumentoResponseDto,
): TipoDocumentoCatalogo {
    return {
        id: String(dto.id),
        codigo: dto.codigo,
        nombre: dto.nombre,
        descripcion: dto.descripcion ?? "",
        activo: dto.activo,
    };
}

export async function listarTiposDocumento(): Promise<TipoDocumentoCatalogo[]> {
    const datos = await apiFetch<TipoDocumentoResponseDto[]>(
        "/api/tipos-documento",
    );
    return datos.map(mapTipoDocumentoResponseDto);
}

/** Catálogo completo (activos e inactivos) para filtros de consulta documental. */
export async function listarTiposDocumentoConsulta(
    esAdmin: boolean,
): Promise<TipoDocumentoCatalogo[]> {
    const path = esAdmin ? "/api/tipos-documento" : "/api/tipos-documento/consulta";
    const datos = await apiFetch<TipoDocumentoResponseDto[]>(path);
    return datos.map(mapTipoDocumentoResponseDto);
}

export async function crearTipoDocumento(
    body: TipoDocumentoRequestDto,
): Promise<TipoDocumentoCatalogo> {
    const dto = await apiFetch<TipoDocumentoResponseDto>("/api/tipos-documento", {
        method: "POST",
        body: JSON.stringify(body),
    });
    return mapTipoDocumentoResponseDto(dto);
}

export async function actualizarTipoDocumento(
    id: string,
    body: TipoDocumentoUpdateRequestDto,
): Promise<TipoDocumentoCatalogo> {
    const dto = await apiFetch<TipoDocumentoResponseDto>(
        `/api/tipos-documento/${id}`,
        {
            method: "PUT",
            body: JSON.stringify(body),
        },
    );
    return mapTipoDocumentoResponseDto(dto);
}

export async function cambiarEstadoTipoDocumento(
    id: string,
    activo: boolean,
): Promise<TipoDocumentoCatalogo> {
    const dto = await apiFetch<TipoDocumentoResponseDto>(
        `/api/tipos-documento/${id}/estado`,
        {
            method: "PATCH",
            body: JSON.stringify({ activo } satisfies TipoDocumentoEstadoRequestDto),
        },
    );
    return mapTipoDocumentoResponseDto(dto);
}
