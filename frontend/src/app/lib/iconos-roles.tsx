import type { LucideIcon } from "lucide-react";

import {
        BriefcaseBusiness,
        Crown,
        ShieldCheck,
        UserRound,
} from "lucide-react";

export interface IconoRol {
        icono: LucideIcon;
        color: string;
}

const color = {
        azul: "text-sky-600",
        verde: "text-emerald-600",
        verdeOscuro: "text-green-700",
        morado: "text-violet-600",
        naranja: "text-orange-600",
        rojo: "text-red-600",
        dorado: "text-amber-500",
        gris: "text-slate-600",
        cian: "text-cyan-600",
};

const ICONOS_ROL: Record<string, IconoRol> = {
        administrador: {
                icono: ShieldCheck,
                color: color.rojo,
        },

        administrativo: {
                icono: BriefcaseBusiness,
                color: color.azul,
        },

        "jefe de area": {
                icono: Crown,
                color: color.verde,
        },


};

export function obtenerIconoRol(nombre: string): IconoRol {
        const clave = nombre
            .trim()
            .toLowerCase()
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "");

        return (
            ICONOS_ROL[clave] ?? {
                    icono: UserRound,
                    color: "text-muted-foreground",
            }
        );
}