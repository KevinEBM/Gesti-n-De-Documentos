export type Rol = "administrador" | "jefe_area" | "administrativo";

export const etiquetaRol: Record<Rol, string> = {
    administrador: "Administrador",
    jefe_area: "Jefe de área",
    administrativo: "Administrativo",
};

export const descripcionRolInicio: Record<Rol, string> = {
    administrador:
        "Puedes administrar usuarios y catálogos, publicar documentos, gestionar nuevas versiones y consultar la documentación disponible en el sistema.",
    jefe_area:
        "Puedes consultar la documentación disponible para tu área y acceder al historial de versiones cuando sea necesario.",
    administrativo:
        "Puedes consultar y descargar la documentación vigente disponible para tu área o de alcance general.",
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
