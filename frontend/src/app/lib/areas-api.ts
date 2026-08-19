import { apiFetch } from "./api";

export interface AreaResponseDto {
    id: number;
    codigo: string;
    nombre: string;
    descripcion: string | null;
    activo: boolean;
    fechaCreacion: string;
    fechaActualizacion: string;
}

export interface AreaRequestDto {
    codigo: string;
    nombre: string;
    descripcion: string;
}

export interface AreaEstadoRequestDto {
    activo: boolean;
}

export interface AreaCatalogo {
    id: string;
    codigo: string;
    nombre: string;
    descripcion: string;
    activo: boolean;
}

export function mapAreaResponseDto(dto: AreaResponseDto): AreaCatalogo {
    return {
        id: String(dto.id),
        codigo: dto.codigo,
        nombre: dto.nombre,
        descripcion: dto.descripcion ?? "",
        activo: dto.activo,
    };
}

export async function listarAreas(): Promise<AreaCatalogo[]> {
    const datos = await apiFetch<AreaResponseDto[]>("/api/areas");
    return datos.map(mapAreaResponseDto);
}

export async function crearArea(body: AreaRequestDto): Promise<AreaCatalogo> {
    const dto = await apiFetch<AreaResponseDto>("/api/areas", {
        method: "POST",
        body: JSON.stringify(body),
    });
    return mapAreaResponseDto(dto);
}

export async function actualizarArea(
    id: string,
    body: AreaRequestDto,
): Promise<AreaCatalogo> {
    const dto = await apiFetch<AreaResponseDto>(`/api/areas/${id}`, {
        method: "PUT",
        body: JSON.stringify(body),
    });
    return mapAreaResponseDto(dto);
}

export async function cambiarEstadoArea(
    id: string,
    activo: boolean,
): Promise<AreaCatalogo> {
    const dto = await apiFetch<AreaResponseDto>(`/api/areas/${id}/estado`, {
        method: "PATCH",
        body: JSON.stringify({ activo } satisfies AreaEstadoRequestDto),
    });
    return mapAreaResponseDto(dto);
}
