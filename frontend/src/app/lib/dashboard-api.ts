import { apiFetch } from "./api";

export interface DashboardAdminResponseDto {
    documentosPublicados: number;
    usuariosActivos: number;
    areasRegistradas: number;
}

export type TipoActividadDto = "NUEVA_PUBLICACION" | "NUEVA_VERSION";

export interface ActividadDocumentalResponseDto {
    tipoActividad: TipoActividadDto;
    documentoId: number;
    codigoDocumento: string;
    tituloDocumento: string;
    numeroVersion: number;
    descripcionCambio: string;
    fechaPublicacion: string;
    publicadoPorId: number;
    publicadoPorNombres: string;
    publicadoPorApellidos: string;
}

export interface DashboardMetricas {
    documentosPublicados: number;
    usuariosActivos: number;
    areasRegistradas: number;
}

export interface ActividadDocumental {
    tipoActividad: TipoActividadDto;
    documentoId: string;
    codigoDocumento: string;
    tituloDocumento: string;
    numeroVersion: number;
    descripcionCambio: string;
    fechaPublicacion: string;
    publicadoPorNombre: string;
}

function mapActividadDocumentalResponseDto(
    dto: ActividadDocumentalResponseDto,
): ActividadDocumental {
    const nombres = dto.publicadoPorNombres?.trim() ?? "";
    const apellidos = dto.publicadoPorApellidos?.trim() ?? "";
    const nombreCompleto = `${nombres} ${apellidos}`.trim();

    return {
        tipoActividad: dto.tipoActividad,
        documentoId: String(dto.documentoId),
        codigoDocumento: dto.codigoDocumento,
        tituloDocumento: dto.tituloDocumento,
        numeroVersion: dto.numeroVersion,
        descripcionCambio: dto.descripcionCambio,
        fechaPublicacion: dto.fechaPublicacion,
        publicadoPorNombre: nombreCompleto || "Usuario no disponible",
    };
}

export async function obtenerDashboard(): Promise<DashboardMetricas> {
    const dto = await apiFetch<DashboardAdminResponseDto>("/api/dashboard");
    return {
        documentosPublicados: dto.documentosPublicados,
        usuariosActivos: dto.usuariosActivos,
        areasRegistradas: dto.areasRegistradas,
    };
}

export async function obtenerActividadReciente(): Promise<ActividadDocumental[]> {
    const dto = await apiFetch<ActividadDocumentalResponseDto[]>(
        "/api/dashboard/actividad-reciente",
    );
    return dto.map(mapActividadDocumentalResponseDto);
}

export function etiquetaTipoActividad(tipo: TipoActividadDto): string {
    switch (tipo) {
        case "NUEVA_PUBLICACION":
            return "Nueva publicación";
        case "NUEVA_VERSION":
            return "Nueva versión";
    }
}
