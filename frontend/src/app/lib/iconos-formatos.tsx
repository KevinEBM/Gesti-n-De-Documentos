import type { Icon } from "@tabler/icons-react";
import type { LucideIcon } from "lucide-react";

import {
    Notebook,
    Landmark,
    CalendarSync,
    FilePen,
    ChartNetwork,
    ClipboardList,
    FileText,
    BookOpen,
    GitBranch,
} from "lucide-react";

import {
    IconTemplate,
    IconCertificate,
    IconClipboardList,
    IconFileInfo,
    IconMapRoute,
    IconFileDescription,
    IconBook,
    IconRoute,
    IconFileCheck,
} from "@tabler/icons-react";

export interface IconosFormatos {
    icono: LucideIcon | Icon;
    color: string;
}

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
    blanco: "text-gray-100",
    morado: "text-purple-500",
};

const ICONOS_FORMATOS: Record<string, IconosFormatos> = {

    // 📄 Plantilla
    plantilla: {
        icono: IconTemplate,
        color: color.azul,
    },

    // 📜 Protocolo
    protocolo: {
        icono: IconCertificate,
        color: color.dorado,
    },

    // 📋 Programa
    programa: {
        icono: IconClipboardList,
        color: color.verdeOscuro,
    },

    // 📘 Manual
    manual: {
        icono: Notebook,
        color: color.azul,
    },

    // 🏛️ Política
    politica: {
        icono: Landmark,
        color: color.morado,
    },

    // 🗺️ Procedimiento
    procedimiento: {
        icono: IconMapRoute,
        color: color.verde,
    },

    // 📝 Formato
    formato: {
        icono: FilePen,
        color: color.piel,
    },

    // 📑 Instructivo
    instructivo: {
        icono: IconFileDescription,
        color: color.rojo,
    },

    // 📖 Guía
    guia: {
        icono: IconBook,
        color: color.cafe,
    },

    // 🔀 Flujograma
    flujograma: {
        icono: GitBranch,
        color: color.morado,
    },
};

export function obtenerIconoFormato(nombre: string): IconosFormatos {

    const clave = nombre
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .trim();

    return (
        ICONOS_FORMATOS[clave] ?? {
            icono: IconFileInfo,
            color: color.metalico,
        }
    );
}