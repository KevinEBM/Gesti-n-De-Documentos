import { mapRolBackend } from "./auth-storage";
import { apiFetch } from "./api";
import {
    mapAreaResponseDto,
    type AreaCatalogo,
    type AreaResponseDto,
} from "./areas-api";
import { etiquetaRol } from "./data";

export type RolBackend = "ADMINISTRADOR" | "JEFE_AREA" | "ADMINISTRATIVO";

export type EstadoUsuario = "ACTIVO" | "INACTIVO" | "BLOQUEADO";

export interface RolResponseDto {
    id: number;
    nombre: RolBackend;
    descripcion: string;
    activo: boolean;
}

export interface UsuarioResponseDto {
    id: number;
    nombres: string;
    apellidos: string;
    correo: string;
    rol: RolResponseDto;
    estado: EstadoUsuario;
    areas: AreaResponseDto[];
    areaPrincipalId: number | null;
    fechaCreacion: string;
    fechaActualizacion: string;
}

export interface UsuarioRequestDto {
    nombres: string;
    apellidos: string;
    correo: string;
    password: string;
    rolId: number;
    areaIds: number[];
    areaPrincipalId?: number | null;
}

export interface UsuarioUpdateRequestDto {
    nombres: string;
    apellidos: string;
    correo: string;
    rolId: number;
    areaIds: number[];
    areaPrincipalId?: number | null;
}

export interface UsuarioEstadoRequestDto {
    estado: EstadoUsuario;
}

export interface RolCatalogo {
    id: string;
    codigo: RolBackend;
    descripcion: string;
    activo: boolean;
    etiqueta: string;
}

export interface UsuarioCatalogo {
    id: string;
    nombres: string;
    apellidos: string;
    nombreCompleto: string;
    correo: string;
    rolId: string;
    rolCodigo: RolBackend;
    rolEtiqueta: string;
    estado: EstadoUsuario;
    areaIds: string[];
    areaPrincipalId: string | null;
    areas: AreaCatalogo[];
}

export function mapRolResponseDto(dto: RolResponseDto): RolCatalogo {
    return {
        id: String(dto.id),
        codigo: dto.nombre,
        descripcion: dto.descripcion,
        activo: dto.activo,
        etiqueta: etiquetaRol[mapRolBackend(dto.nombre)],
    };
}

export function mapUsuarioResponseDto(dto: UsuarioResponseDto): UsuarioCatalogo {
    const areas = dto.areas.map(mapAreaResponseDto);

    return {
        id: String(dto.id),
        nombres: dto.nombres,
        apellidos: dto.apellidos,
        nombreCompleto: `${dto.nombres} ${dto.apellidos}`.trim(),
        correo: dto.correo,
        rolId: String(dto.rol.id),
        rolCodigo: dto.rol.nombre,
        rolEtiqueta: etiquetaRol[mapRolBackend(dto.rol.nombre)],
        estado: dto.estado,
        areaIds: areas.map((area) => area.id),
        areaPrincipalId:
            dto.areaPrincipalId != null ? String(dto.areaPrincipalId) : null,
        areas,
    };
}

export async function listarUsuarios(): Promise<UsuarioCatalogo[]> {
    const datos = await apiFetch<UsuarioResponseDto[]>("/api/usuarios");
    return datos.map(mapUsuarioResponseDto);
}

export async function obtenerUsuario(id: string): Promise<UsuarioCatalogo> {
    const dto = await apiFetch<UsuarioResponseDto>(`/api/usuarios/${id}`);
    return mapUsuarioResponseDto(dto);
}

export async function crearUsuario(
    body: UsuarioRequestDto,
): Promise<UsuarioCatalogo> {
    const dto = await apiFetch<UsuarioResponseDto>("/api/usuarios", {
        method: "POST",
        body: JSON.stringify(body),
    });
    return mapUsuarioResponseDto(dto);
}

export async function actualizarUsuario(
    id: string,
    body: UsuarioUpdateRequestDto,
): Promise<UsuarioCatalogo> {
    const dto = await apiFetch<UsuarioResponseDto>(`/api/usuarios/${id}`, {
        method: "PUT",
        body: JSON.stringify(body),
    });
    return mapUsuarioResponseDto(dto);
}

export async function cambiarEstadoUsuario(
    id: string,
    estado: EstadoUsuario,
): Promise<UsuarioCatalogo> {
    const dto = await apiFetch<UsuarioResponseDto>(`/api/usuarios/${id}/estado`, {
        method: "PATCH",
        body: JSON.stringify({ estado } satisfies UsuarioEstadoRequestDto),
    });
    return mapUsuarioResponseDto(dto);
}

export async function listarRoles(): Promise<RolCatalogo[]> {
    const datos = await apiFetch<RolResponseDto[]>("/api/roles");
    return datos.map(mapRolResponseDto);
}
