import type { LucideIcon } from "lucide-react";

import {
    HardHat, Leaf,
    ClipboardList,
    Cog,
    MessageCircleQuestionMark
} from "lucide-react";

import {IconBuildingWarehouse,
        IconTruckDelivery,
        IconShoppingCart,
        IconBanana,
        IconAutomation,
        IconTopologyStarRing3,
        IconDeviceImacCode,
        IconBuildingStore,
        IconUserShield,
        IconSearch,
        IconBusinessplan,
        IconPasswordFingerprint,
        IconHeartHandshake,
        IconSchool,
        IconUsersGroup,
        IconFirstAidKit,
        IconBiohazard,
        IconDroplet,
        IconBugOff,
        IconFilter,
        IconTrash,
        IconSpray,
        IconShieldCog,
        IconWashHand,
        IconShieldX,
        IconArchive,
        IconMapSearch,
        IconTestPipe2,
        IconRotateClockwise2,
        IconSettings,
        IconSTurnRight,
        IconShieldCheck,
        IconBan,
        IconBulb,
        IconArrowUpRightCircle,
        IconPackageExport,
        IconMap,
        IconTruck,
        IconCurrencyDollar,
        IconTools,
        IconTool,
        IconBuilding,
        IconBuildingFactory2,
        IconShield,
        IconDeviceDesktopAnalytics,
        IconBriefcase,
        IconCalculator,
        Icon

} from "@tabler/icons-react"
    
export interface IconoSubProceso {
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
    cafe: "text-amber-900",
    negro: "text-black",
    amarillo: "text-yellow-600",
    rosa: "text-pink-500",
    lima: "text-lime-500",
    indigio: "text-indigo-600",
    teal: "text-teal-500",
    fucsia: "text-fuchsia-500",
    azulClaro: "text-blue-400",
    oliva: "text-lime-700",
    marron: "text-rose-700",
    celeste: "text-sky-400",
    bordó: "text-red-900",
    piel: "text-orange-300",
    verdeClaro: "text-lime-500",
    metalico: "text-slate-500",
    grisOscuro: "text-gray-700",
    grisClaro: "text-gray-300",
    rosado: "text-pink-500",
    rosadoClaro: "text-pink-300",
    turquesa: "text-teal-500",
    cyan: "text-cyan-500",
    azulOscuro: "text-indigo-700",
    violeta: "text-violet-600",
    verdeMar: "text-teal-700",
    plateado: "text-zinc-400",

};

const ICONOS_SUBPROCESO = {

/* Gestión Humana */

    "capacitacion y desarrollo": {
        icono: IconSchool,
        color: color.verde,
    },

    "gestion humana": {
        icono: IconUsersGroup,
        color: color.piel,
    },

    "salud y seguridad en el trabajo": {
        icono: IconFirstAidKit,
        color: color.azulClaro,
    },

    "sustancias quimicas": {
        icono: IconBiohazard,
        color: color.verdeOscuro,
    },

    "programa de respeto, convivencia e inclusion": {
        icono: IconHeartHandshake,
        color: color.naranja,
    },

/* Gestión Ambiental */

    "calidad del agua potable": {
        icono: IconDroplet,
        color: color.azul,
    },

    "control de plagas": {
        icono: IconBugOff,
        color: color.rojo,
    },

    "control de residuos liquidos": {
        icono:IconFilter,
        color: color.amarillo,
    },

    "control de residuos solidos": {
        icono: IconTrash,
        color: color.negro,
    },

    "limpieza y desinfeccion": {
        icono: IconSpray,
        color: color.celeste,
    },

    "gestion ambiental": {
            icono: Leaf,
            color: color.verde,
    },

    "maquinaria y equipo":{
                icono:Cog,
                color: color.cafe
    },

/* Gestión de Calidad */

    "auditoria interna": {
        icono: IconShieldCog,
        color: color.verdeOscuro,
    },

    "buenas practicas higienicas": {
        icono: IconWashHand,
        color: color.verde,
    },

    "control de alergenos": {
        icono:IconShieldX,
        color: color.dorado
    },
    "gestion documental": {
        icono: IconArchive,
        color: color.cian,
    },

    "gestion de la calidad": {
        icono: IconShieldCheck,
        color: color.verdeClaro,
    },

    "plan de muestreo": {
        icono: IconTestPipe2,
        color: color.verdeOscuro,
    },

    "material extrano": {
        icono: IconSearch,
        color: color.rojo,
    },

    recall: {
        icono: IconRotateClockwise2,
        color: color.amarillo,
    },

    "sistemas integrados de gestion": {
        icono: IconSettings,
        color: color.naranja,
    },

    trazabilidad: {
        icono: IconSTurnRight,
        color: color.verdeMar,
    },

    "programa de producto no conforme": {
        icono: IconBan,
        color: color.azulOscuro,
    },

    "desarrollo e innovacion": {
        icono: IconBulb,
        color: color.celeste,
    },
    "proceso de mejora continua": {
        icono: IconArrowUpRightCircle,
        color: color.azul,
    },
    "programa de peticiones quejas y reclamos": {
        icono: MessageCircleQuestionMark,
        color: color.rojo,
    },

/* Gestión Logística */
    "almacen y abastecimiento de insumos": {
      icono: IconBuildingWarehouse,
        color:color.marron,
    },
    logistica: {
        icono: IconMap,
        color: color.azul,
    },

    transporte: {
        icono: IconTruckDelivery,
        color: color.verde
    },

     "programa de almacenamiento": {
        icono: IconPackageExport,
        color: color.rojo,
    },

    /* Gestión Comercial */

    "gestion comercial y ventas": {
        icono:IconCurrencyDollar,
        color: color.verde,
    },

    /* Gestión de Producción */

    "gestion de la produccion" : {
        icono: IconBuildingFactory2,
        color: color.verdeOscuro,
    },

    /* Gestión de Mantenimiento */

    "calibracion y verificacion de equipos de medicion": {
        icono: IconTools,
        color: color.azul,
    },

    "mantenimiento de maquinaria y equipos": {
        icono: IconTool,
        color: color.verde,
    },

    "mantenimiento de edificios e instalaciones": {
        icono: IconBuilding,
        color: color.gris,
    },

    /* Gestión de Seguridad Física */

    "seguridad y vigilancia": {
        icono: IconShield,
        color: color.verde,
    },

    /* Gestión TICs */

    "tecnologia informatica y de comunicaciones": {
        icono: IconDeviceDesktopAnalytics,
        color: color.rojo,
    },

    /* Gestión Financiera */

    "gestion administrativa": {
        icono: IconBriefcase,
        color: color.marron,
    },

    "gestion contable y financiera": {
        icono: IconCalculator,
        color: color.gris,
    },

    /* Gestión de Compras */

    "control de proveedores": {
        icono: IconBuildingStore,
        color: color.verde,
    },

    "gestion de compras": {
        icono: IconShoppingCart,
        color: color.rojo,
    }
};



export function obtenerIconoSubProceso(nombre: string): IconoSubProceso {
    const clave = nombre
        .trim()
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "");

    return (
        ICONOS_SUBPROCESO[clave] ?? {
            icono: ClipboardList,
            color: "text-muted-foreground",
        }
    );
}
