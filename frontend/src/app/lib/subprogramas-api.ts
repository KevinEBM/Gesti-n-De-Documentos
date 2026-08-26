import { apiFetch } from "./api";
import type { AreaResponseDto } from "./areas-api";

export interface SubprogramaResponseDto {
    id: number;
    codigo: string;
    nombre: string;
    descripcion: string | null;
    area: AreaResponseDto;
    activo: boolean;
    fechaCreacion: string;
    fechaActualizacion: string;
}

export interface SubprogramaRequestDto {
    codigo: string;
    nombre: string;
    descripcion: string;
    areaId: number;
}

export interface SubprogramaUpdateRequestDto {
    codigo: string;
    nombre: string;
    descripcion: string;
    areaId: number;
}

export interface SubprogramaEstadoRequestDto {
    activo: boolean;
}

export interface SubprogramaCatalogo {
    id: string;
    codigo: string;
    nombre: string;
    descripcion: string;
    areaId: string;
    areaCodigo: string;
    areaNombre: string;
    areaActiva: boolean;
    activo: boolean;
}

export function mapSubprogramaResponseDto(
    dto: SubprogramaResponseDto,
): SubprogramaCatalogo {
    return {
        id: String(dto.id),
        codigo: dto.codigo,
        nombre: dto.nombre,
        descripcion: dto.descripcion ?? "",
        areaId: String(dto.area.id),
        areaCodigo: dto.area.codigo,
        areaNombre: dto.area.nombre,
        areaActiva: dto.area.activo,
        activo: dto.activo,
    };
}

export async function listarSubprogramas(): Promise<SubprogramaCatalogo[]> {
    const datos = await apiFetch<SubprogramaResponseDto[]>("/api/subprogramas");
    return datos.map(mapSubprogramaResponseDto);
}

export async function crearSubprograma(
    body: SubprogramaRequestDto,
): Promise<SubprogramaCatalogo> {
    const dto = await apiFetch<SubprogramaResponseDto>("/api/subprogramas", {
        method: "POST",
        body: JSON.stringify(body),
    });
    return mapSubprogramaResponseDto(dto);
}

export async function actualizarSubprograma(
    id: string,
    body: SubprogramaUpdateRequestDto,
): Promise<SubprogramaCatalogo> {
    const dto = await apiFetch<SubprogramaResponseDto>(`/api/subprogramas/${id}`, {
        method: "PUT",
        body: JSON.stringify(body),
    });
    return mapSubprogramaResponseDto(dto);
}

export async function cambiarEstadoSubprograma(
    id: string,
    activo: boolean,
): Promise<SubprogramaCatalogo> {
    const dto = await apiFetch<SubprogramaResponseDto>(
        `/api/subprogramas/${id}/estado`,
        {
            method: "PATCH",
            body: JSON.stringify({ activo } satisfies SubprogramaEstadoRequestDto),
        },
    );
    return mapSubprogramaResponseDto(dto);
}
