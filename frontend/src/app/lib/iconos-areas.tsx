import type { LucideIcon } from "lucide-react";
import {
    Users,
    Leaf,
    CircleCheckBig,
    PackageSearch,
    BriefcaseBusiness,
    Factory,
    Wrench,
    ShieldUser,
    MonitorCog,
    Wallet,
    ShoppingCart,
} from "lucide-react";

export interface IconoArea {
    icono: LucideIcon;
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
    morado: "text-purple-500"
};

const ICONOS_AREAS: Record<string, IconoArea> = {
    "gestion humana": {
        icono: Users,
        color: color.piel,
    },

    "gestion ambiental": {
        icono: Leaf,
        color: color.verdeOscuro,
    },

    "gestion de calidad": {
        icono: CircleCheckBig,
        color: color.verdeClaro,
    },

    "gestion logistica": {
        icono: PackageSearch,
        color: color.metalico,
    },

    "gestion comercial": {
        icono: BriefcaseBusiness,
        color: color.negro,
    },

    "gestion de produccion": {
        icono: Factory,
        color: color.rojo,
    },

    "gestion de mantenimiento": {
        icono: Wrench,
        color: color.azul,
    },

    "gestion de seguridad fisica": {
        icono: ShieldUser,
        color: color.verde,
    },

    "gestion tics": {
        icono: MonitorCog,
        color: color.dorado,
    },

    "gestion financiera": {
        icono: Wallet,
        color: color.cafe,
    },

    "gestion de compras": {
        icono: ShoppingCart,
        color: color.morado,
    },
};

export function obtenerIconoArea(nombre: string): IconoArea {
    const clave = nombre
        .trim()
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "");

    return (
        ICONOS_AREAS[clave] ?? {
            icono: BriefcaseBusiness,
            color: "text-muted-foreground",
        }
    );
}