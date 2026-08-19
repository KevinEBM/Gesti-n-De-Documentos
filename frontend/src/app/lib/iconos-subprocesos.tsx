import {
    BadgeCheck, BrushCleaning,
    Calculator,
    GraduationCap,
    HardHat, Leaf,
    LucideIcon,
    ShoppingCart,
    Wallet,
    Wrench,
    Warehouse,
    PencilRuler,
    ClipboardList,
    FolderArchive,
    ShieldCheck,
    Cog
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
        School
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

    inocuidad: {
        icono: School,
        color: color.verde,
    },

    documental: {
        icono: FolderArchive,
        color: color.azul,
    },

    calidad: {
        icono: BadgeCheck,
        color: color.rojo,
    },

    nomina: {
        icono: Wallet,
        color: color.verdeOscuro,
    },

    sst: {
        icono: HardHat,
        color: color.naranja,
    },

    capacitacion: {
        icono: GraduationCap,
        color: color.morado,
    },

    "mantenimiento locativo": {
        icono: Wrench,
        color: color.rojo,
    },

    "maquinaria y equipo":{
        icono:Cog,
        color: color.cafe
    },

    calibracion: {
        icono:PencilRuler,
        color: color.amarillo
    },

    compras: {
        icono: ShoppingCart,
        color: color.rojo,
    },

    contabilidad: {
        icono: Calculator,
        color: color.verdeOscuro,
    },

    ambiental: {
        icono: Leaf,
        color: color.verde,
    },

    "limpieza y desinfeccion (lyd)": {
        icono: BrushCleaning,
        color: color.azul,
    },

    abastecimiento: {
        icono: IconBuildingWarehouse,
        color: color.verde,
    },

    proveedores: {
        icono:IconHeartHandshake,
        color: color.dorado
    },
    transporte: {
        icono: IconTruckDelivery,
        color: color.cian,
    },

    "centro de distribucion (cedi)": {
        icono: Warehouse,
        color: color.rojo
    },

    logistica: {
        icono: IconHomeSearch,
        color: color.naranja
    },

    comercial: {
        icono: IconShoppingCart,
        color: color.rojo,
    },

    "peladero y recepcion": {
        icono: IconBanana,
        color: color.amarillo
    },

    produccion: {
        icono: IconAutomation,
        color: color.rojo
    },

    costos: {
        icono: IconBusinessplan,
        color: color.verde
    },

    nexo: {
        icono: IconTopologyStarRing3,
        color: color.azul
    },

    tecnologia: {
        icono: IconDeviceImacCode,
        color: color.rojo
    },

    cyberseguridad: {
      icono: IconPasswordFingerprint,
        color:color.verde
    },
    tesoreria: {
        icono: IconBuildingBank,
        color: color.azul
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
