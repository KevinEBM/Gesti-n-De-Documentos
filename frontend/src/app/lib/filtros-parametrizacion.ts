import type { AreaCatalogo } from "./areas-api";
import {
    listarAbreviaturasPorNombreTipo,
    nombresCoinciden,
    resolverNombreTipo,
} from "./extraer-info-doc";
import { normalizarNombre } from "./normalizar-nombre";
import type { SubprogramaCatalogo } from "./subprogramas-api";
import type { TipoDocumentoCatalogo } from "./tipos-documento-api";

export type FiltroEstadoActivo = "todos" | "activos" | "inactivos";

export function coincideBusquedaEnCampos(
    busqueda: string,
    campos: Array<string | null | undefined>,
): boolean {
    const termino = busqueda.trim();
    if (!termino) return true;
    const normalizado = normalizarNombre(termino);
    return campos.some((campo) => {
        if (campo == null || campo === "") return false;
        return normalizarNombre(campo).includes(normalizado);
    });
}

export function cumpleFiltroEstadoActivo(
    activo: boolean,
    estado: FiltroEstadoActivo,
): boolean {
    if (estado === "activos") return activo;
    if (estado === "inactivos") return !activo;
    return true;
}

export function filtrarAreasCatalogo(
    areas: AreaCatalogo[],
    busqueda: string,
    estado: FiltroEstadoActivo,
): AreaCatalogo[] {
    return areas.filter(
        (area) =>
            cumpleFiltroEstadoActivo(area.activo, estado) &&
            coincideBusquedaEnCampos(busqueda, [area.nombre, area.codigo]),
    );
}

export function filtrarSubprogramasCatalogo(
    subprogramas: SubprogramaCatalogo[],
    busqueda: string,
    estado: FiltroEstadoActivo,
): SubprogramaCatalogo[] {
    return subprogramas.filter(
        (subprograma) =>
            cumpleFiltroEstadoActivo(subprograma.activo, estado) &&
            coincideBusquedaEnCampos(busqueda, [
                subprograma.codigo,
                subprograma.nombre,
                subprograma.areaCodigo,
                subprograma.areaNombre,
            ]),
    );
}

export function coincideBusquedaTipoDocumento(
    tipo: TipoDocumentoCatalogo,
    busqueda: string,
): boolean {
    if (coincideBusquedaEnCampos(busqueda, [tipo.codigo, tipo.nombre])) {
        return true;
    }

    const termino = busqueda.trim();
    if (!termino) return true;

    const nombrePorAbreviatura = resolverNombreTipo(termino);
    if (nombrePorAbreviatura && nombresCoinciden(nombrePorAbreviatura, tipo.nombre)) {
        return true;
    }

    const normalizado = normalizarNombre(termino);
    return listarAbreviaturasPorNombreTipo(tipo.nombre).some((abreviatura) =>
        normalizarNombre(abreviatura).includes(normalizado),
    );
}

export function filtrarTiposDocumentoCatalogo(
    tipos: TipoDocumentoCatalogo[],
    busqueda: string,
    estado: FiltroEstadoActivo,
): TipoDocumentoCatalogo[] {
    return tipos.filter(
        (tipo) =>
            cumpleFiltroEstadoActivo(tipo.activo, estado) &&
            coincideBusquedaTipoDocumento(tipo, busqueda),
    );
}
