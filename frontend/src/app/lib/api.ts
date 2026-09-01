import { invalidarSesionSiNoAutorizada } from "./auth-sesion-invalida";
import { getToken } from "./auth-storage";

export const API_BASE_URL =
    import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export interface ApiResponse<T> {
    exito: boolean;
    mensaje: string | null;
    datos: T | null;
    errores: Record<string, string> | null;
    fechaHora: string;
}

export interface LoginResponseDto {
    token: string;
    tipo: string;
    id: number;
    correo: string;
    nombres: string;
    apellidos: string;
    rol: string;
    areaPrincipalId: number | null;
    areaPrincipalNombre: string | null;
}

export interface PerfilUsuarioResponseDto {
    id: number;
    correo: string;
    nombres: string;
    apellidos: string;
    rol: string;
    areaPrincipalId: number | null;
    areaPrincipalNombre: string | null;
}

export class ApiError extends Error {
    status: number;
    errores?: Record<string, string>;

    constructor(status: number, mensaje: string, errores?: Record<string, string>) {
        super(mensaje);
        this.name = "ApiError";
        this.status = status;
        this.errores = errores;
    }
}

function lanzarApiError(
    status: number,
    path: string,
    method: string | undefined,
    mensaje: string,
    errores?: Record<string, string>,
): never {
    invalidarSesionSiNoAutorizada(status, path, method);
    throw new ApiError(status, mensaje, errores);
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
    const headers = new Headers(init.headers);
    const token = getToken();

    if (token && !headers.has("Authorization")) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    const body = init.body;
    const isFormData = typeof FormData !== "undefined" && body instanceof FormData;

    if (body && !isFormData && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }

    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...init,
        headers,
    });

    let payload: ApiResponse<T> | null = null;

    try {
        payload = (await response.json()) as ApiResponse<T>;
    } catch {
        if (!response.ok) {
            lanzarApiError(response.status, path, init.method, `Error HTTP ${response.status}`);
        }
        throw new ApiError(response.status, "La respuesta del servidor no es JSON valido");
    }

    if (!response.ok) {
        lanzarApiError(
            response.status,
            path,
            init.method,
            payload?.mensaje ?? `Error HTTP ${response.status}`,
            payload?.errores ?? undefined,
        );
    }

    if (!payload.exito) {
        lanzarApiError(
            response.status,
            path,
            init.method,
            payload.mensaje ?? "La operacion no fue exitosa",
            payload.errores ?? undefined,
        );
    }

    return payload.datos as T;
}
