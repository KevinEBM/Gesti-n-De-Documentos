import {

    HardHat, Leaf,
    LucideIcon,
    ClipboardList,

    Cog,
    Search,
} from "lucide-react";

import {IconBuildingWarehouse,
        IconTruckDelivery,
        IconShoppingCart,
        IconBanana,
        IconAutomation,
        IconTopologyStarRing3,
        IconDeviceImacCode,
        IconBuildingBank,
        IconUserShield,
        IconHomeSearch,
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
    negro: "text-black-600",
    amarillo: "text-yellow-600",
};

const ICONOS_SUBPROCESO = {

/* Gestión Humana */

    "capacitacion y desarrollo": {
        icono: IconSchool,
        color: color.verde,
    },

    "gestion humana": {
        icono: IconUsersGroup,
        color: color.azul,
    },

    "salud y seguridad en el trabajo": {
        icono: IconFirstAidKit,
        color: color.rojo,
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
        color: color.azul,
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
        color: color.rojo,
    },
/*
    "material extraño": {
        icono: IconMapSearch,
        color: color.naranja,
    },*/

    "plan de muestreo": {
        icono: IconTestPipe2,
        color: color.rojo,
    },

    recall: {
        icono: IconRotateClockwise2,
        color: color.amarillo,
    },

    "sistemas integrados de gestion": {
        icono: IconSettings,
        color: color.rojo,
    },

    trazabilidad: {
        icono: IconSTurnRight,
        color: color.verde,
    },

    "programa de producto no conforme": {
        icono: IconBan,
        color: color.azul,
    },

    "desarrollo e innovacion": {
        icono: IconBulb,
        color: color.rojo,
    },
    "proceso de mejora continua": {
        icono: IconArrowUpRightCircle,
        color: color.azul,
    },


    cyberseguridad: {
      icono: IconPasswordFingerprint,
        color:color.verde,
    },
    tesoreria: {
        icono: IconBuildingBank,
        color: color.azul,
    },

    "seguridad fisica": {
        icono: IconUserShield,
        color: color.verde
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
