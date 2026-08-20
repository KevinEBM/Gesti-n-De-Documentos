import { apiFetch } from "./api";

export interface CambiarContrasenaRequestDto {
    contrasenaActual: string;
    nuevaContrasena: string;
    confirmacionContrasena: string;
}

export async function cambiarContrasena(body: CambiarContrasenaRequestDto): Promise<void> {
    await apiFetch<void>("/api/auth/contrasena", {
        method: "PUT",
        body: JSON.stringify(body),
    });
}
