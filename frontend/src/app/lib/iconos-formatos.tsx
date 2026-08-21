import type { Icon } from "@tabler/icons-react";
import type { LucideIcon } from "lucide-react";
import { FilePen, Landmark, Notebook, Workflow } from "lucide-react";
import {
    IconCertificate,
    IconClipboardList,
    IconFileChart,
    IconFileDescription,
    IconFileInfo,
    IconFileSettings,
    IconFileX,
    IconInfoCircle,
    IconListCheck,
    IconMapRoute,
    IconTemplate,
} from "@tabler/icons-react";

import { normalizarNombre } from "./normalizar-nombre";

export interface IconosFormatos {
    icono: LucideIcon | Icon;
    color: string;
}

export type IconoTipoDocumento = IconosFormatos;

const color = {
    piel: "text-orange-300",
    verdeOscuro: "text-green-700",
    verdeClaro: "text-lime-500",
    metalico: "text-slate-500",
    negro: "text-slate-900 dark:text-white",
    rojo: "text-red-600",
    azul: "text-blue-600",
    verde: "text-emerald-600",
    dorado: "text-amber-500",
    cafe: "text-amber-700",
    morado: "text-purple-500",
    gris: "text-slate-600",
};

const ICONOS_TIPOS_DOCUMENTO: Record<string, IconosFormatos> = {
    manual: { icono: Notebook, color: color.azul },
    programa: { icono: IconClipboardList, color: color.verdeOscuro },
    proceso: { icono: Workflow, color: color.verde },
    procedimiento: { icono: IconMapRoute, color: color.verde },
    politica: { icono: Landmark, color: color.morado },
    reglamento: { icono: IconListCheck, color: color.cafe },
    caracterizacion: { icono: IconInfoCircle, color: color.azul },
    instructivo: { icono: IconFileDescription, color: color.rojo },
    protocolo: { icono: IconCertificate, color: color.dorado },
    formato: { icono: FilePen, color: color.piel },
    "ficha tecnica": { icono: IconFileSettings, color: color.metalico },
    diagrama: { icono: IconFileChart, color: color.morado },
    "otros documentos": { icono: IconFileX, color: color.gris },

    // Extra visual opcional (no pertenece al catálogo institucional de 13)
    plantilla: { icono: IconTemplate, color: color.azul },
};

const FALLBACK_TIPO: IconosFormatos = {
    icono: IconFileInfo,
    color: color.metalico,
};

export function obtenerIconoFormato(nombre: string): IconosFormatos {
    return ICONOS_TIPOS_DOCUMENTO[normalizarNombre(nombre)] ?? FALLBACK_TIPO;
}

/** Alias semántico alineado con tipos de documento del catálogo. */
export const obtenerIconoTipoDocumento = obtenerIconoFormato;
