// Datos ficticios de demostración. No contienen información real ni confidencial.

import {
    CircleCheckBig,
    Factory,
    Leaf,
    MonitorCog,
    PackageSearch,
    ShieldUser,
    ShoppingCart,
    Truck,
    Users,
    Wallet,
    Wrench,
} from "lucide-react";

export type Estado = "publicado" | "borrador" | "inactivo";

export interface Area {
    id: string;
    nombre: string;
    descripcion: string;
    activo: boolean;
}

export interface SubProceso {
    id: string;
    nombre: string;
    descripcion: string;
    areaId: string;
    activo: boolean;
}

export interface TipoDocumento {
    id: string;
    nombre: string;
    descripcion: string;
    activo: boolean;
}

export type Rol = "administrador" | "jefe_area" | "administrativo";

export const etiquetaRol: Record<Rol, string> = {
    administrador: "Administrador",
    jefe_area: "Jefe de área",
    administrativo: "Administrativo",
};

export interface Version {
    numero: string;
    fecha: string;
    autor: string;
    notas: string;
    archivo?: string;
}

export interface Usuario {
    id: string;
    nombre: string;
    correo: string;
    areaId: string;
    rol: Rol;
    activo: boolean;
}

export interface Documento {
    codigo: string;
    id: string;
    nombre: string;
    descripcion: string;
    archivo: string;
    areaId: string;
    visibleTodas: boolean;
    areasAutorizadas: string[];
    subProcesoId: string;
    tipoId: string;
    version: string;
    fechaPublicacion: string;
    estado: Estado;
    publicadoPor: string;
    consultas: number;
    versiones: Version[];
}

export interface Notificacion {
    id: string;
    tipo: "nuevo" | "version";
    titulo: string;
    mensaje: string;
    fecha: string;
    leida: boolean;
    documentoId: string;
}

export interface Actividad {
    id: string;
    usuario: string;
    accion: string;
    detalle: string;
    fecha: string;
}

/* =======================================================
 * ÁREAS
 * ======================================================= */

export const areasIniciales: Area[] = [
    {
        id: "a1",
        nombre: "Gestión Humana",
        descripcion: "Gestión del personal y bienestar",
        activo: true,
    },
    {
        id: "a2",
        nombre: "Gestión Ambiental",
        descripcion: "Contabilidad, tesorería y presupuesto",
        activo: true,
    },
    {
        id: "a3",
        nombre: "Gestión de Calidad",
        descripcion: "Infraestructura y sistemas de información",
        activo: true,
    },
    {
        id: "a4",
        nombre: "Gestión Logística",
        descripcion: "Sistema de gestión de calidad",
        activo: true,
    },
    {
        id: "a5",
        nombre: "Gestión Comercial",
        descripcion: "Procesos operativos y logística",
        activo: true,
    },
    {
        id: "a6",
        nombre: "Gestión de Producción",
        descripcion: "Gestión de impactos ambientales y sostenibilidad",
        activo: true,
    },
    {
        id: "a7",
        nombre: "Gestión de Mantenimiento",
        descripcion: "Control de procesos de fabricación y manufactura",
        activo: true,
    },
    {
        id: "a8",
        nombre: "Gestión de Seguridad Física",
        descripcion: "Estrategia, programación y control de proyectos",
        activo: true,
    },
    {
        id: "a9",
        nombre: "Gestión TICs",
        descripcion: "Gestión de cadena de suministro y distribución",
        activo: true,
    },
    {
        id: "a10",
        nombre: "Gestión Financiera",
        descripcion: "Seguridad industrial y salud ocupacional",
        activo: true,
    },
    {
        id: "a11",
        nombre: "Gestión de Compras",
        descripcion: "Mantenimiento preventivo y correctivo de equipos",
        activo: true,
    },
];

/* =======================================================
  SUBPROCESOS

  IMPORTANTE:
  Las abreviaturas NO se almacenan aquí.
  Se mantienen exclusivamente en la lógica
  de extracción de documentos.
  ======================================================= */

export const subProcesosIniciales: SubProceso[] = [
    // Área Gestión Humana
    {
        id: "c1",
        nombre: "Capacitación y Desarrollo",
        descripcion:
            "Planificación, ejecución y seguimiento de actividades de capacitación y desarrollo de competencias del personal.",
        areaId: "a1",
        activo: true,
    },
    {
        id: "c2",
        nombre: "Gestión Humana",
        descripcion:
            "Gestión de los procesos relacionados con el personal, incluyendo selección, vinculación, bienestar, capacitación y seguimiento.",
        areaId: "a1",
        activo: true,
    },
    {
        id: "c3",
        nombre: "Salud y Seguridad en el Trabajo",
        descripcion:
            "Gestión de actividades destinadas a prevenir accidentes laborales, enfermedades ocupacionales y riesgos asociados al trabajo.",
        areaId: "a1",
        activo: true,
    },
    {
        id: "c4",
        nombre: "Sustancias Químicas",
        descripcion:
            "Gestión, almacenamiento, manipulación, identificación y control de sustancias químicas utilizadas en la organización.",
        areaId: "a1",
        activo: true,
    },
    {
        id: "c5",
        nombre: "Programa de Respeto, Convivencia e Inclusión",
        descripcion:
            "Promoción y seguimiento de prácticas orientadas al respeto, la convivencia, el buen trato y la inclusión dentro de la organización.",
        areaId: "a1",
        activo: true,
    },

    // Área Gestión Ambiental
    {
        id: "c6",
        nombre: "Calidad del Agua Potable",
        descripcion:
            "Control y seguimiento de la calidad del agua potable utilizada en las instalaciones y procesos de la organización.",
        areaId: "a2",
        activo: true,
    },
    {
        id: "c7",
        nombre: "Control de Plagas",
        descripcion:
            "Prevención, control y seguimiento de plagas para garantizar condiciones adecuadas de higiene e inocuidad.",
        areaId: "a2",
        activo: true,
    },
    {
        id: "c8",
        nombre: "Control de Residuos Líquidos",
        descripcion:
            "Gestión, control y seguimiento de la generación, manejo y disposición adecuada de residuos líquidos.",
        areaId: "a2",
        activo: true,
    },
    {
        id: "c9",
        nombre: "Control de Residuos Sólidos",
        descripcion:
            "Gestión, clasificación, almacenamiento, disposición y seguimiento de los residuos sólidos generados en la organización.",
        areaId: "a2",
        activo: true,
    },
    {
        id: "c10",
        nombre: "Limpieza y Desinfección",
        descripcion:
            "Planificación, ejecución y seguimiento de las actividades de limpieza y desinfección de instalaciones, equipos y áreas.",
        areaId: "a2",
        activo: true,
    },
    {
        id: "c11",
        nombre: "Gestión Ambiental",
        descripcion:
            "Planificación, control y seguimiento de las actividades relacionadas con la protección del medio ambiente y el cumplimiento de requisitos ambientales.",
        areaId: "a2",
        activo: true,
    },

    // Área Gestión de Calidad
    {
        id: "c12",
        nombre: "Auditoría Interna",
        descripcion:
            "Planificación, ejecución y seguimiento de auditorías internas para verificar el cumplimiento de los procesos y requisitos establecidos.",
        areaId: "a3",
        activo: true,
    },
    {
        id: "c13",
        nombre: "Buenas Prácticas Higiénicas",
        descripcion:
            "Implementación y seguimiento de prácticas de higiene, limpieza y manipulación para garantizar condiciones sanitarias adecuadas.",
        areaId: "a3",
        activo: true,
    },
    {
        id: "c14",
        nombre: "Control de Alérgenos",
        descripcion:
            "Identificación, prevención y control de riesgos asociados a la presencia de alérgenos en los procesos y productos.",
        areaId: "a3",
        activo: true,
    },
    {
        id: "c15",
        nombre: "Gestión Documental",
        descripcion:
            "Administración, control, actualización, almacenamiento, distribución y conservación de la documentación de la organización.",
        areaId: "a3",
        activo: true,
    },
    {
        id: "c16",
        nombre: "Gestión de la Calidad",
        descripcion:
            "Planificación, implementación, seguimiento y mejora de los procesos relacionados con la gestión de la calidad.",
        areaId: "a3",
        activo: true,
    },
    {
        id: "c17",
        nombre: "Material Extraño",
        descripcion:
            "Prevención, identificación, control y seguimiento de materiales extraños que puedan afectar la calidad o inocuidad de los productos.",
        areaId: "a3",
        activo: true,
    },
    {
        id: "c18",
        nombre: "Plan de Muestreo",
        descripcion:
            "Planificación, ejecución y seguimiento de actividades de muestreo para verificar la calidad e inocuidad de productos y procesos.",
        areaId: "a3",
        activo: true,
    },
    {
        id: "c19",
        nombre: "Recall",
        descripcion:
            "Gestión y seguimiento del retiro de productos del mercado cuando se identifica un riesgo o incumplimiento que pueda afectar al consumidor.",
        areaId: "a3",
        activo: true,
    },
    {
        id: "c20",
        nombre: "Sistemas Integrados de Gestión",
        descripcion:
            "Integración, administración y seguimiento de los sistemas de gestión implementados en la organización.",
        areaId: "a3",
        activo: true,
    },
    {
        id: "c21",
        nombre: "Trazabilidad",
        descripcion:
            "Identificación, registro y seguimiento del recorrido de materias primas, insumos, productos y procesos a lo largo de la cadena operativa.",
        areaId: "a3",
        activo: true,
    },
    {
        id: "c22",
        nombre: "Programa de Producto No Conforme",
        descripcion:
            "Identificación, clasificación, control, tratamiento y seguimiento de productos que no cumplen los requisitos establecidos.",
        areaId: "a3",
        activo: true,
    },
    {
        id: "c23",
        nombre: "Desarrollo e Innovación",
        descripcion:
            "Planificación y ejecución de iniciativas orientadas al desarrollo, innovación y mejora de productos, procesos y servicios.",
        areaId: "a3",
        activo: true,
    },
    {
        id: "c24",
        nombre: "Proceso de Mejora Continua",
        descripcion:
            "Identificación, análisis, implementación y seguimiento de oportunidades de mejora para optimizar los procesos de la organización.",
        areaId: "a3",
        activo: true,
    },

    // Área Gestión Logistica
    {
        id: "c25",
        nombre: "Almacén y Abastecimiento de Insumos",
        descripcion:
            "Gestión de recepción, almacenamiento, control y abastecimiento de insumos necesarios para la operación.",
        areaId: "a4",
        activo: true,
    },
    {
        id: "c26",
        nombre: "Logística",
        descripcion:
            "Planificación, coordinación y control de las actividades relacionadas con almacenamiento, distribución, despacho y movimiento de mercancías.",
        areaId: "a4",
        activo: true,
    },
    {
        id: "c27",
        nombre: "Transporte",
        descripcion:
            "Gestión, planificación y seguimiento del transporte de mercancías, productos, materiales e insumos.",
        areaId: "a4",
        activo: true,
    },
    {
        id: "c28",
        nombre: "Programa de Almacenamiento",
        descripcion:
            "Planificación y control de las condiciones, procedimientos y prácticas necesarias para el adecuado almacenamiento de productos, materiales e insumos.",
        areaId: "a4",
        activo: true,
    },

    // Área Gestión Comercial
    {
        id: "c29",
        nombre: "Gestión Comercial y Ventas",
        descripcion:
            "Planificación, coordinación y seguimiento de las actividades comerciales, ventas y atención de clientes.",
        areaId: "a5",
        activo: true,
    },

    // Área Gestión de Producción
    {
        id: "c30",
        nombre: "Gestión de la Producción",
        descripcion:
            "Planificación, coordinación y seguimiento de las actividades relacionadas con el proceso productivo.",
        areaId: "a6",
        activo: true,
    },

    // Área Gestión de Mantenimiento
    {
        id: "c31",
        nombre: "Calibración y Verificación de Equipos de Medición",
        descripcion:
            "Control, seguimiento y verificación de la calibración de equipos e instrumentos utilizados en los procesos.",
        areaId: "a7",
        activo: true,
    },
    {
        id: "c32",
        nombre: "Mantenimiento de Maquinaria y Equipos",
        descripcion:
            "Planificación, ejecución y seguimiento del mantenimiento preventivo y correctivo de maquinaria y equipos.",
        areaId: "a7",
        activo: true,
    },
    {
        id: "c33",
        nombre: "Mantenimiento de Edificios e Instalaciones",
        descripcion:
            "Gestión del mantenimiento preventivo y correctivo de edificios, instalaciones e infraestructura física.",
        areaId: "a7",
        activo: true,
    },

    // Área Gestión de Seguridad Fisica
    {
        id: "c34",
        nombre: "Seguridad y Vigilancia",
        descripcion:
            "Control y seguimiento de las actividades de seguridad, vigilancia y protección de las instalaciones, personas y bienes.",
        areaId: "a8",
        activo: true,
    },

    // Área Gestión TICs
    {
        id: "c35",
        nombre: "Tecnología Informática y de Comunicaciones",
        descripcion:
            "Administración, soporte y mantenimiento de los sistemas, equipos, redes y servicios tecnológicos y de comunicaciones.",
        areaId: "a9",
        activo: true,
    },

    // Área Gestión Financiera
    {
        id: "c36",
        nombre: "Gestión Administrativa",
        descripcion:
            "Administración y coordinación de las actividades operativas, documentales y administrativas necesarias para el funcionamiento del área.",
        areaId: "a10",
        activo: true,
    },
    {
        id: "c37",
        nombre: "Gestión Contable y Financiera",
        descripcion:
            "Registro, control y seguimiento de las operaciones contables, financieras y económicas de la organización.",
        areaId: "a10",
        activo: true,
    },

    // Área Gestión de Compras
    {
        id: "c38",
        nombre: "Control de Proveedores",
        descripcion:
            "Evaluación, selección, seguimiento y control de proveedores de bienes y servicios de la organización.",
        areaId: "a11",
        activo: true,
    },
    {
        id: "c39",
        nombre: "Gestión de Compras",
        descripcion:
            "Planificación, solicitud, adquisición y seguimiento de bienes, insumos, materiales y servicios requeridos por la organización.",
        areaId: "a11",
        activo: true,
    },
    {
        id: "c40",
        nombre: "Programa de Peticiones Quejas y Reclamos",
        descripcion:
            "Lorem imput solem",
        areaId: "a3",
        activo: true,
    }
];

/* =======================================================
 * VISUALIZACIÓN DE ÁREAS
 * ======================================================= */

export const areaVisual = {
    a1: { icon: Users, color: "#D2A679" },
    a2: { icon: Leaf, color: "#2E7D32" },
    a3: { icon: CircleCheckBig, color: "#66BB6A" },
    a4: { icon: Truck, color: "#78909C" },
    a5: { icon: ShoppingCart, color: "#212121" },
    a6: { icon: Factory, color: "#C62828" },
    a7: { icon: Wrench, color: "#1976D2" },
    a8: { icon: ShieldUser, color: "#2E7D32" },
    a9: { icon: MonitorCog, color: "#D4AF37" },
    a10: { icon: Wallet, color: "#6D4C41" },
    a11: { icon: PackageSearch, color: "#FF00FF" },
};

/* =======================================================
  TIPOS DE DOCUMENTO

  IMPORTANTE:
  Las abreviaturas utilizadas para extraer documentos
  NO se almacenan en estos objetos.
  ======================================================= */

export const tiposIniciales: TipoDocumento[] = [

    {
        id: "t1",
        nombre: "Manual",
        descripcion:
            "Documento que describe de manera detallada las instrucciones, lineamientos y pautas para realizar determinadas actividades.",
        activo: true,
    },
    {
        id: "t2",
        nombre: "Programa",
        descripcion:
            "Documento que describe de manera general las actividades, responsables, entradas y salidas que conforman un proceso.",
        activo: true,
    },
    {
        id: "t3",
        nombre: "Proceso",
        descripcion:
            "Documento que establece los pasos, responsables y condiciones necesarias para ejecutar una actividad o proceso.",
        activo: true,
    },
    {
        id: "t4",
        nombre: "Procedimiento",
        descripcion:
            "Secuencia formal y detallada de acciones para realizar un proceso.",
        activo: true,
    },
    {
        id: "t5",
        nombre: "Politica",
        descripcion:
            "Principios, compromisos y lineamientos generales que orientan la actuación. ",
        activo: true,
    },
    {
        id: "t6",
        nombre: "Reglamento",
        descripcion:
            "Normas, reglas y disposiciones que deben cumplirse en la organización.",
        activo: true,
    },
    {
        id: "t7",
        nombre: "Caracterización",
        descripcion:
            "Documento que identifica y describe elementos principales de un proceso (entradas, actividades, salidas, responsables, recursos).",
        activo: true,
    },
    {
        id: "t8",
        nombre: "Instructivo",
        descripcion:
            "Instrucciones específicas y detalladas para realizar correctamente una actividad.",
        activo: true,
    },
    {
        id: "t9",
        nombre: "Protocolo",
        descripcion:
            "Secuencia formal de acciones y criterios de actuación ante una situación específica.",
        activo: true,
    },
    {
        id: "t10",
        nombre: "Formato",
        descripcion:
            "Documento estructurado para registrar y recopilar información durante la ejecución de una actividad.",
        activo: true,
    },
    {
        id: "t11",
        nombre: "Ficha Técnica",
        descripcion:
            "Información técnica, características y especificaciones de un producto, equipo o proceso.",
        activo: true,
    },
    {
        id: "t12",
        nombre: "Diagrama",
        descripcion:
            "Representación gráfica de un proceso, flujo o estructura.",
        activo: true,
    },
    {
        id: "t13",
        nombre: "Otros Documentos",
        descripcion:
            "No se acomoda a la clasificación presentada",
        activo: true,
    },
];

/* =======================================================
  USUARIOS
  ======================================================= */

export const usuariosIniciales: Usuario[] = [
    {
        id: "u1",
        nombre: "Laura Restrepo",
        correo: "admin@empresa.com",
        areaId: "a3",
        rol: "administrador",
        activo: true,
    },
    {
        id: "u2",
        nombre: "Carlos Medina",
        correo: "administrativo@empresa.com",
        areaId: "a1",
        rol: "administrativo",
        activo: true,
    },
    {
        id: "u3",
        nombre: "Ana Gutiérrez",
        correo: "jefearea@empresa.com",
        areaId: "a2",
        rol: "jefe_area",
        activo: true,
    },
    {
        id: "u4",
        nombre: "Jorge Salas",
        correo: "jorge.salas@empresa.com",
        areaId: "a4",
        rol: "jefe_area",
        activo: true,
    },
    {
        id: "u5",
        nombre: "Marcela Ríos",
        correo: "marcela.rios@empresa.com",
        areaId: "a1",
        rol: "administrativo",
        activo: false,
    },
    {
        id: "u6",
        nombre: "Diego Peñaranda",
        correo: "diego.penaranda@empresa.com",
        areaId: "a3",
        rol: "administrador",
        activo: true,
    },
];

/* =======================================================
 * DOCUMENTOS
 * ======================================================= */

export const documentosIniciales: Documento[] = [
    {
        codigo: "PC-mnk-oi-01",
        id: "d1",
        nombre: "Manual de inducción y reinducción",
        descripcion:
            "Documento guía para el proceso de ingreso de nuevos colaboradores, deberes, derechos y rutas de acompañamiento.",
        archivo: "manual-induccion-v3.pdf",
        areaId: "a1",
        visibleTodas: true,
        areasAutorizadas: [],
        subProcesoId: "c1",
        tipoId: "t4",
        version: "3.0",
        fechaPublicacion: "2026-07-18",
        estado: "publicado",
        publicadoPor: "Laura Restrepo",
        consultas: 248,
        versiones: [
            {
                numero: "3.0",
                fecha: "2026-07-18",
                autor: "Laura Restrepo",
                notas: "Actualización de rutas de acompañamiento.",
            },
            {
                numero: "2.1",
                fecha: "2026-03-02",
                autor: "Laura Restrepo",
                notas: "Ajuste de anexos y formatos.",
            },
            {
                numero: "1.0",
                fecha: "2025-09-11",
                autor: "Diego Peñaranda",
                notas: "Versión inicial.",
            },
        ],
    },
    {
        codigo: "pp-mpk-ji-02",
        id: "d2",
        nombre: "Política de tratamiento de datos personales",
        descripcion:
            "Lineamientos institucionales para la recolección, uso y custodia de datos personales.",
        archivo: "politica-datos-v2.pdf",
        areaId: "a3",
        visibleTodas: true,
        areasAutorizadas: [],
        subProcesoId: "c2",
        tipoId: "t5",
        version: "2.0",
        fechaPublicacion: "2026-07-10",
        estado: "publicado",
        publicadoPor: "Diego Peñaranda",
        consultas: 412,
        versiones: [
            {
                numero: "2.0",
                fecha: "2026-07-10",
                autor: "Diego Peñaranda",
                notas: "Se incorpora el capítulo de proveedores.",
            },
            {
                numero: "1.0",
                fecha: "2025-05-20",
                autor: "Diego Peñaranda",
                notas: "Versión inicial.",
            },
        ],
    },
    {
        codigo: "p3-mmm-oa-09",
        id: "d3",
        nombre: "Procedimiento de solicitud de vacaciones",
        descripcion:
            "Pasos, tiempos y responsables para tramitar el disfrute de vacaciones.",
        archivo: "procedimiento-vacaciones-v1.pdf",
        areaId: "a1",
        visibleTodas: false,
        areasAutorizadas: ["a1", "a2"],
        subProcesoId: "c1",
        tipoId: "t6",
        version: "1.2",
        fechaPublicacion: "2026-06-28",
        estado: "publicado",
        publicadoPor: "Laura Restrepo",
        consultas: 331,
        versiones: [
            {
                numero: "1.2",
                fecha: "2026-06-28",
                autor: "Laura Restrepo",
                notas: "Se reduce el tiempo de aprobación.",
            },
            {
                numero: "1.0",
                fecha: "2026-01-15",
                autor: "Laura Restrepo",
                notas: "Versión inicial.",
            },
        ],
    },
    {
        codigo: "as-dfg-hj-01",
        id: "d4",
        nombre: "Plantilla de acta de reunión",
        descripcion:
            "Formato estándar para el registro de reuniones internas y compromisos.",
        archivo: "plantilla-acta-v1.docx",
        areaId: "a4",
        visibleTodas: true,
        areasAutorizadas: [],
        subProcesoId: "c5",
        tipoId: "t1",
        version: "1.0",
        fechaPublicacion: "2026-06-12",
        estado: "publicado",
        publicadoPor: "Jorge Salas",
        consultas: 189,
        versiones: [
            {
                numero: "1.0",
                fecha: "2026-06-12",
                autor: "Jorge Salas",
                notas: "Versión inicial.",
            },
        ],
    },
    {
        codigo: "qw-ert-yu-1",
        id: "d5",
        nombre: "Protocolo de respuesta ante incidentes de seguridad",
        descripcion:
            "Actuación escalonada frente a incidentes que comprometan la información institucional.",
        archivo: "protocolo-incidentes-v2.pdf",
        areaId: "a3",
        visibleTodas: false,
        areasAutorizadas: ["a3", "a4"],
        subProcesoId: "c2",
        tipoId: "t2",
        version: "2.1",
        fechaPublicacion: "2026-07-22",
        estado: "publicado",
        publicadoPor: "Diego Peñaranda",
        consultas: 97,
        versiones: [
            {
                numero: "2.1",
                fecha: "2026-07-22",
                autor: "Diego Peñaranda",
                notas: "Nuevos canales de reporte.",
            },
            {
                numero: "2.0",
                fecha: "2026-02-09",
                autor: "Diego Peñaranda",
                notas: "Reestructuración por niveles.",
            },
        ],
    },
    {
        codigo: "qw-ert-yu-12",
        id: "d6",
        nombre: "Programa de bienestar laboral 2026",
        descripcion:
            "Actividades de bienestar, cronograma y responsables para la vigencia 2026.",
        archivo: "programa-bienestar-2026.pdf",
        areaId: "a1",
        visibleTodas: true,
        areasAutorizadas: [],
        subProcesoId: "c1",
        tipoId: "t3",
        version: "1.0",
        fechaPublicacion: "2026-07-25",
        estado: "publicado",
        publicadoPor: "Laura Restrepo",
        consultas: 156,
        versiones: [
            {
                numero: "1.0",
                fecha: "2026-07-25",
                autor: "Laura Restrepo",
                notas: "Versión inicial.",
            },
        ],
    },
    {
        codigo: "qw-ert-yu-13",
        id: "d7",
        nombre: "Manual de ejecución presupuestal",
        descripcion:
            "Criterios y controles para la ejecución del presupuesto anual.",
        archivo: "manual-presupuesto-v4.pdf",
        areaId: "a10",
        visibleTodas: false,
        areasAutorizadas: ["a2", "a4"],
        subProcesoId: "c3",
        tipoId: "t4",
        version: "4.0",
        fechaPublicacion: "2026-05-30",
        estado: "publicado",
        publicadoPor: "Ana Gutiérrez",
        consultas: 210,
        versiones: [
            {
                numero: "4.0",
                fecha: "2026-05-30",
                autor: "Ana Gutiérrez",
                notas: "Actualización de topes de gasto.",
            },
            {
                numero: "3.0",
                fecha: "2025-06-01",
                autor: "Ana Gutiérrez",
                notas: "Vigencia anterior.",
            },
        ],
    },
    {
        codigo: "qw-ert-yu-14",
        id: "d8",
        nombre: "Política de teletrabajo",
        descripcion:
            "Condiciones, requisitos y deberes aplicables a la modalidad de teletrabajo.",
        archivo: "politica-teletrabajo-v1.pdf",
        areaId: "a1",
        visibleTodas: false,
        areasAutorizadas: ["a1"],
        subProcesoId: "c5",
        tipoId: "t5",
        version: "0.9",
        fechaPublicacion: "2026-07-27",
        estado: "borrador",
        publicadoPor: "Laura Restrepo",
        consultas: 12,
        versiones: [
            {
                numero: "0.9",
                fecha: "2026-07-27",
                autor: "Laura Restrepo",
                notas: "Borrador en revisión jurídica.",
            },
        ],
    },
    {
        codigo: "qw-ert-yu-15",
        id: "d9",
        nombre: "Procedimiento de auditoría interna",
        descripcion:
            "Planeación, ejecución y cierre de auditorías internas de calidad.",
        archivo: "procedimiento-auditoria-v2.pdf",
        areaId: "a4",
        visibleTodas: false,
        areasAutorizadas: ["a4", "a2"],
        subProcesoId: "c4",
        tipoId: "t6",
        version: "2.0",
        fechaPublicacion: "2026-04-18",
        estado: "publicado",
        publicadoPor: "Jorge Salas",
        consultas: 143,
        versiones: [
            {
                numero: "2.0",
                fecha: "2026-04-18",
                autor: "Jorge Salas",
                notas: "Se agregan listas de verificación.",
            },
            {
                numero: "1.0",
                fecha: "2025-04-10",
                autor: "Jorge Salas",
                notas: "Versión inicial.",
            },
        ],
    },
    {
        codigo: "qw-ert-yu-16",
        id: "d10",
        nombre: "Plantilla de informe de gestión",
        descripcion:
            "Formato para la entrega trimestral de informes por área.",
        archivo: "plantilla-informe-v2.docx",
        areaId: "a4",
        visibleTodas: false,
        areasAutorizadas: ["a1", "a3"],
        subProcesoId: "c4",
        tipoId: "t1",
        version: "2.0",
        fechaPublicacion: "2026-03-14",
        estado: "inactivo",
        publicadoPor: "Jorge Salas",
        consultas: 64,
        versiones: [
            {
                numero: "2.0",
                fecha: "2026-03-14",
                autor: "Jorge Salas",
                notas: "Reemplazada por el tablero digital.",
            },
        ],
    },
    {
        codigo: "qw-ert-yu-17",
        id: "d11",
        nombre: "Protocolo de atención al usuario interno",
        descripcion:
            "Estándares de atención, tiempos de respuesta y escalamiento de solicitudes.",
        archivo: "protocolo-atencion-v1.pdf",
        areaId: "a2",
        visibleTodas: false,
        areasAutorizadas: ["a2"],
        subProcesoId: "c5",
        tipoId: "t2",
        version: "1.1",
        fechaPublicacion: "2026-07-05",
        estado: "publicado",
        publicadoPor: "Ana Gutiérrez",
        consultas: 88,
        versiones: [
            {
                numero: "1.1",
                fecha: "2026-07-05",
                autor: "Ana Gutiérrez",
                notas: "Ajuste de tiempos de respuesta.",
            },
            {
                numero: "1.0",
                fecha: "2025-11-02",
                autor: "Ana Gutiérrez",
                notas: "Versión inicial.",
            },
        ],
    },
    {
        codigo: "qw-ert-yu-18",
        id: "d12",
        nombre: "Programa de capacitación técnica",
        descripcion:
            "Rutas de formación técnica para equipos de tecnología y operaciones.",
        archivo: "programa-capacitacion-v1.pdf",
        areaId: "a3",
        visibleTodas: false,
        areasAutorizadas: ["a3", "a1"],
        subProcesoId: "c1",
        tipoId: "t3",
        version: "1.0",
        fechaPublicacion: "2026-06-20",
        estado: "publicado",
        publicadoPor: "Diego Peñaranda",
        consultas: 121,
        versiones: [
            {
                numero: "1.0",
                fecha: "2026-06-20",
                autor: "Diego Peñaranda",
                notas: "Versión inicial.",
            },
        ],
    },
];

/* =======================================================
 * NOTIFICACIONES
 * ======================================================= */

export const notificacionesIniciales: Notificacion[] = [
    {
        id: "n1",
        tipo: "nuevo",
        titulo: "Nuevo documento publicado",
        mensaje:
            "Programa de bienestar laboral 2026 fue publicado por Talento Humano.",
        fecha: "2026-07-25 09:14",
        leida: false,
        documentoId: "d6",
    },
    {
        id: "n2",
        tipo: "version",
        titulo: "Nueva versión disponible",
        mensaje:
            "Manual de inducción y reinducción pasó a la versión 3.0.",
        fecha: "2026-07-18 15:40",
        leida: false,
        documentoId: "d1",
    },
    {
        id: "n3",
        tipo: "version",
        titulo: "Nueva versión disponible",
        mensaje:
            "Protocolo de respuesta ante incidentes de seguridad pasó a la versión 2.1.",
        fecha: "2026-07-22 11:05",
        leida: false,
        documentoId: "d5",
    },
    {
        id: "n4",
        tipo: "version",
        titulo: "Nueva versión disponible",
        mensaje:
            "Procedimiento de solicitud de vacaciones pasó a la versión 1.2.",
        fecha: "2026-06-28 08:22",
        leida: true,
        documentoId: "d3",
    },
    {
        id: "n5",
        tipo: "nuevo",
        titulo: "Nuevo documento publicado",
        mensaje:
            "Plantilla de acta de reunión ya está disponible en la biblioteca.",
        fecha: "2026-06-12 16:31",
        leida: true,
        documentoId: "d4",
    },
];

/* =======================================================
 * ACTIVIDAD
 * ======================================================= */

export const actividadInicial: Actividad[] = [
    {
        id: "ac1",
        usuario: "Laura Restrepo",
        accion: "Publicó documento",
        detalle: "Programa de bienestar laboral 2026",
        fecha: "2026-07-25 09:14",
    },
    {
        id: "ac2",
        usuario: "Diego Peñaranda",
        accion: "Publicó nueva versión",
        detalle: "Protocolo de respuesta ante incidentes v2.1",
        fecha: "2026-07-22 11:05",
    },
    {
        id: "ac3",
        usuario: "Laura Restrepo",
        accion: "Creó usuario",
        detalle: "jorge.salas@empresa.com",
        fecha: "2026-07-20 10:02",
    },
    {
        id: "ac4",
        usuario: "Diego Peñaranda",
        accion: "Actualizó categoría",
        detalle: "Seguridad de la información",
        fecha: "2026-07-19 17:45",
    },
    {
        id: "ac5",
        usuario: "Laura Restrepo",
        accion: "Publicó nueva versión",
        detalle: "Manual de inducción v3.0",
        fecha: "2026-07-18 15:40",
    },
    {
        id: "ac6",
        usuario: "Jorge Salas",
        accion: "Cambió estado",
        detalle: "Plantilla de informe de gestión → inactivo",
        fecha: "2026-07-14 09:00",
    },
];