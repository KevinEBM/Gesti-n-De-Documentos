export type Rol = "administrador" | "jefe_area" | "administrativo";

export const etiquetaRol: Record<Rol, string> = {
    administrador: "Administrador",
    jefe_area: "Jefe de área",
    administrativo: "Administrativo",
};

export interface Usuario {
    id: string;
    nombre: string;
    correo: string;
    areaId?: string;
    areaPrincipalNombre?: string | null;
    rol: Rol;
    activo: boolean;
}
