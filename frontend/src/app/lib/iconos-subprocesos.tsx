import type { Icon } from "@tabler/icons-react";
import type { LucideIcon } from "lucide-react";
import {
    AlertTriangle,
    BadgeCheck,
    BrushCleaning,
    Bug,
    Calculator,
    ClipboardCheck,
    ClipboardList,
    Cog,
    Droplet,
    Droplets,
    FlaskConical,
    FolderArchive,
    GraduationCap,
    HardHat,
    Leaf,
    Lightbulb,
    MessageSquare,
    PencilRuler,
    RotateCcw,
    Route,
    Search,
    ShieldCheck,
    ShoppingCart,
    Trash2,
    TrendingUp,
    Users,
    Warehouse,
    Wrench,
} from "lucide-react";
import {
    IconAutomation,
    IconBuildingWarehouse,
    IconBusinessplan,
    IconDeviceImacCode,
    IconHeartHandshake,
    IconHomeSearch,
    IconShoppingCart,
    IconTopologyStarRing3,
    IconTruckDelivery,
    IconUserShield,
    IconCircleX,
} from "@tabler/icons-react";

import { normalizarNombre } from "./normalizar-nombre";

export interface IconoSubProceso {
    icono: LucideIcon | Icon;
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
    cafe: "text-amber-900",
    amarillo: "text-yellow-600",
    piel: "text-orange-300",
};

const ICONOS_SUBPROCESO: Record<string, IconoSubProceso> = {
    // Gestión Humana
    "capacitacion y desarrollo": { icono: GraduationCap, color: color.morado },
    "gestion humana": { icono: Users, color: color.piel },
    "salud y seguridad en el trabajo": { icono: HardHat, color: color.naranja },
    "sustancias quimicas": { icono: FlaskConical, color: color.morado },
    "programa de respeto, convivencia e inclusion": {
        icono: IconHeartHandshake,
        color: color.dorado,
    },

    // Gestión Ambiental
    "calidad del agua potable": { icono: Droplets, color: color.azul },
    "control de plagas": { icono: Bug, color: color.verde },
    "control de residuos liquidos": { icono: Droplet, color: color.cian },
    "control de residuos solidos": { icono: Trash2, color: color.gris },
    "limpieza y desinfeccion": { icono: BrushCleaning, color: color.azul },
    "gestion ambiental": { icono: Leaf, color: color.verde },

    // Gestión de Calidad
    "auditoria interna": { icono: ClipboardCheck, color: color.azul },
    "buenas practicas higienicas": { icono: ShieldCheck, color: color.verde },
    "control de alergenos": { icono: AlertTriangle, color: color.naranja },
    "gestion documental": { icono: FolderArchive, color: color.azul },
    "gestion de la calidad": { icono: BadgeCheck, color: color.rojo },
    "material extrano": { icono: Search, color: color.amarillo },
    "plan de muestreo": { icono: ClipboardList, color: color.gris },
    recall: { icono: RotateCcw, color: color.rojo },
    "sistemas integrados de gestion": { icono: IconTopologyStarRing3, color: color.azul },
    trazabilidad: { icono: Route, color: color.verde },
    "programa de producto no conforme": { icono: IconCircleX, color: color.rojo },
    "desarrollo e innovacion": { icono: Lightbulb, color: color.dorado },
    "proceso de mejora continua": { icono: TrendingUp, color: color.verde },
    "programa de peticiones quejas y reclamos": { icono: MessageSquare, color: color.morado },

    // Gestión Logística
    "almacen y abastecimiento de insumos": { icono: IconBuildingWarehouse, color: color.verde },
    logistica: { icono: IconHomeSearch, color: color.naranja },
    transporte: { icono: IconTruckDelivery, color: color.cian },
    "programa de almacenamiento": { icono: Warehouse, color: color.rojo },

    // Gestión Comercial
    "gestion comercial y ventas": { icono: IconShoppingCart, color: color.rojo },

    // Gestión de Producción
    "gestion de la produccion": { icono: IconAutomation, color: color.rojo },

    // Gestión de Mantenimiento
    "calibracion y verificacion de equipos de medicion": {
        icono: PencilRuler,
        color: color.amarillo,
    },
    "mantenimiento de maquinaria y equipos": { icono: Cog, color: color.cafe },
    "mantenimiento de edificios e instalaciones": { icono: Wrench, color: color.rojo },

    // Gestión de Seguridad Física
    "seguridad y vigilancia": { icono: IconUserShield, color: color.verde },

    // Gestión TICs
    "tecnologia informatica y de comunicaciones": { icono: IconDeviceImacCode, color: color.rojo },

    // Gestión Financiera
    "gestion administrativa": { icono: IconBusinessplan, color: color.verde },
    "gestion contable y financiera": { icono: Calculator, color: color.verdeOscuro },

    // Gestión de Compras
    "control de proveedores": { icono: IconHeartHandshake, color: color.dorado },
    "gestion de compras": { icono: ShoppingCart, color: color.rojo },

    // Extra visual opcional (no pertenece al catálogo institucional de 40)
    "maquinaria y equipo": { icono: Cog, color: color.cafe },
};

const FALLBACK_SUBPROCESO: IconoSubProceso = {
    icono: ClipboardList,
    color: "text-muted-foreground",
};

export function obtenerIconoSubProceso(nombre: string): IconoSubProceso {
    return ICONOS_SUBPROCESO[normalizarNombre(nombre)] ?? FALLBACK_SUBPROCESO;
}

/** Alias semántico alineado con la terminología visible Subproceso. */
export const obtenerIconoSubproceso = obtenerIconoSubProceso;
