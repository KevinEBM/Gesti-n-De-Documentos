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
    Workflow
} from "lucide-react";

import {
    IconTemplate,
    IconCertificate,
    IconClipboardList,
    IconFileInfo,
    IconMapRoute,
    IconFileDescription,
    IconBook,
    IconListCheck,
    IconInfoCircle,
    IconFileSettings,
    IconFileChart,
    IconFileX,
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
    morado: "text-purple-500",
    amarillo: "text-yellow-500",
    grisOscuro: "text-gray-700",
    grisClaro: "text-gray-300",
    rosado: "text-pink-500",
    rosadoClaro: "text-pink-300",
    turquesa: "text-teal-500",
    cyan: "text-cyan-500",
    celeste: "text-sky-400",
    azulOscuro: "text-indigo-700",
    violeta: "text-violet-600",
    fucsia: "text-fuchsia-500",
    verdeMar: "text-teal-700",
    plateado: "text-zinc-400",
    naranja: "text-orange-500",

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

    proceso: {
        icono: Workflow,
        color: color.verdeClaro,
    },

    // 📋 Programa
    programa: {
        icono: IconClipboardList,
        color: color.verdeOscuro,
    },

    // 📘 Manual
    manual: {
        icono: Notebook,
        color: color.celeste,
    },

    reglamento: {
        icono: IconListCheck,
        color: color.naranja,
    },

    caracterizacion: {
        icono: IconInfoCircle,
        color: color.amarillo,
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
        color: color.verdeMar,
    },

    "ficha tecnica": {
        icono: IconFileSettings,
        color:color.metalico,
    },

    // 📖 Guía
    diagrama: {
        icono: IconFileChart,
        color: color.cafe,
    },

    // 🔀 Flujograma
    "otros documentos": {
        icono: IconFileX,
        color: color.rojo,
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