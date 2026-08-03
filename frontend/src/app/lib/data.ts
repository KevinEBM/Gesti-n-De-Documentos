// Datos ficticios de demostración. No contienen información real ni confidencial.

export type Estado = "publicado" | "borrador" | "inactivo";

export interface Area {
    id: string;
    nombre: string;
    descripcion: string;
    activo: boolean;
}

export interface Categoria {
    id: string;
    nombre: string;
    descripcion: string;
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

export interface Usuario {
    id: string;
    nombre: string;
    correo: string;
    areaId: string;
    rol: Rol;
    activo: boolean;
}

export interface Version {
    numero: string;
    fecha: string;
    autor: string;
    notas: string;
    archivo?: string;
}

export interface Documento {
    id: string;
    nombre: string;
    descripcion: string;
    archivo: string;
    areaId: string;
    visibleTodas: boolean;
    areasAutorizadas: string[];
    categoriaId: string;
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

export const areasIniciales: Area[] = [
    { id: "a1", nombre: "Talento Humano", descripcion: "Gestión del personal y bienestar", activo: true },
    { id: "a2", nombre: "Financiera", descripcion: "Contabilidad, tesorería y presupuesto", activo: true },
    { id: "a3", nombre: "Tecnología", descripcion: "Infraestructura y sistemas de información", activo: true },
    { id: "a4", nombre: "Calidad", descripcion: "Sistema de gestión de calidad", activo: true },
    { id: "a5", nombre: "Operaciones", descripcion: "Procesos operativos y logística", activo: true },
    { id: "a6", nombre: "Ambiental",descripcion: "input solem",activo:true},
    { id: "a7", nombre: "Ambiental2",descripcion: "input solem2",activo:true},
    { id: "a8", nombre: "Ambiental3",descripcion: "input solem3",activo:true},
    { id: "a9", nombre: "Ambiental4",descripcion: "input solem4",activo:true},

];

export const categoriasIniciales: Categoria[] = [
    { id: "c1", nombre: "Gestión del talento", descripcion: "Documentos de personal", activo: true },
    { id: "c2", nombre: "Seguridad de la información", descripcion: "Lineamientos de seguridad", activo: true },
    { id: "c3", nombre: "Gestión financiera", descripcion: "Documentos contables y presupuestales", activo: true },
    { id: "c4", nombre: "Mejora continua", descripcion: "Auditorías e indicadores", activo: true },
    { id: "c5", nombre: "Documentación general", descripcion: "Documentos transversales", activo: true },
];

export const tiposIniciales: TipoDocumento[] = [
    { id: "t1", nombre: "Plantilla", descripcion: "Formato base reutilizable", activo: true },
    { id: "t2", nombre: "Protocolo", descripcion: "Secuencia formal de actuación", activo: true },
    { id: "t3", nombre: "Programa", descripcion: "Conjunto de actividades planificadas", activo: true },
    { id: "t4", nombre: "Manual", descripcion: "Guía detallada de uso", activo: true },
    { id: "t5", nombre: "Política", descripcion: "Lineamiento institucional", activo: true },
    { id: "t6", nombre: "Procedimiento", descripcion: "Pasos detallados de un proceso", activo: true },
];

export const usuariosIniciales: Usuario[] = [
    { id: "u1", nombre: "Laura Restrepo", correo: "admin@empresa.com", areaId: "a3", rol: "administrador", activo: true },
    { id: "u2", nombre: "Carlos Medina", correo: "administrativo@empresa.com", areaId: "a1", rol: "administrativo", activo: true },
    { id: "u3", nombre: "Ana Gutiérrez", correo: "ana.gutierrez@empresa.com", areaId: "a2", rol: "jefe_area", activo: true },
    { id: "u4", nombre: "Jorge Salas", correo: "jorge.salas@empresa.com", areaId: "a4", rol: "jefe_area", activo: true },
    { id: "u5", nombre: "Marcela Ríos", correo: "marcela.rios@empresa.com", areaId: "a1", rol: "administrativo", activo: false },
    { id: "u6", nombre: "Diego Peñaranda", correo: "diego.penaranda@empresa.com", areaId: "a3", rol: "administrador", activo: true },
];

export const documentosIniciales: Documento[] = [
    {
        id: "d1",
        nombre: "Manual de inducción y reinducción",
        descripcion:
            "Documento guía para el proceso de ingreso de nuevos colaboradores, deberes, derechos y rutas de acompañamiento.",
        archivo: "manual-induccion-v3.pdf",
        areaId: "a1",
        visibleTodas: true,
        areasAutorizadas: [],
        categoriaId: "c1",
        tipoId: "t4",
        version: "3.0",
        fechaPublicacion: "2026-07-18",
        estado: "publicado",
        publicadoPor: "Laura Restrepo",
        consultas: 248,
        versiones: [
            { numero: "3.0", fecha: "2026-07-18", autor: "Laura Restrepo", notas: "Actualización de rutas de acompañamiento." },
            { numero: "2.1", fecha: "2026-03-02", autor: "Laura Restrepo", notas: "Ajuste de anexos y formatos." },
            { numero: "1.0", fecha: "2025-09-11", autor: "Diego Peñaranda", notas: "Versión inicial." },
        ],
    },
    {
        id: "d2",
        nombre: "Política de tratamiento de datos personales",
        descripcion: "Lineamientos institucionales para la recolección, uso y custodia de datos personales.",
        archivo: "politica-datos-v2.pdf",
        areaId: "a3",
        visibleTodas: true,
        areasAutorizadas: [],
        categoriaId: "c2",
        tipoId: "t5",
        version: "2.0",
        fechaPublicacion: "2026-07-10",
        estado: "publicado",
        publicadoPor: "Diego Peñaranda",
        consultas: 412,
        versiones: [
            { numero: "2.0", fecha: "2026-07-10", autor: "Diego Peñaranda", notas: "Se incorpora el capítulo de proveedores." },
            { numero: "1.0", fecha: "2025-05-20", autor: "Diego Peñaranda", notas: "Versión inicial." },
        ],
    },
    {
        id: "d3",
        nombre: "Procedimiento de solicitud de vacaciones",
        descripcion: "Pasos, tiempos y responsables para tramitar el disfrute de vacaciones.",
        archivo: "procedimiento-vacaciones-v1.pdf",
        areaId: "a1",
        visibleTodas: false,
        areasAutorizadas: ["a1", "a2"],
        categoriaId: "c1",
        tipoId: "t6",
        version: "1.2",
        fechaPublicacion: "2026-06-28",
        estado: "publicado",
        publicadoPor: "Laura Restrepo",
        consultas: 331,
        versiones: [
            { numero: "1.2", fecha: "2026-06-28", autor: "Laura Restrepo", notas: "Se reduce el tiempo de aprobación." },
            { numero: "1.0", fecha: "2026-01-15", autor: "Laura Restrepo", notas: "Versión inicial." },
        ],
    },
    {
        id: "d4",
        nombre: "Plantilla de acta de reunión",
        descripcion: "Formato estándar para el registro de reuniones internas y compromisos.",
        archivo: "plantilla-acta-v1.docx",
        areaId: "a4",
        visibleTodas: true,
        areasAutorizadas: [],
        categoriaId: "c5",
        tipoId: "t1",
        version: "1.0",
        fechaPublicacion: "2026-06-12",
        estado: "publicado",
        publicadoPor: "Jorge Salas",
        consultas: 189,
        versiones: [{ numero: "1.0", fecha: "2026-06-12", autor: "Jorge Salas", notas: "Versión inicial." }],
    },
    {
        id: "d5",
        nombre: "Protocolo de respuesta ante incidentes de seguridad",
        descripcion: "Actuación escalonada frente a incidentes que comprometan la información institucional.",
        archivo: "protocolo-incidentes-v2.pdf",
        areaId: "a3",
        visibleTodas: false,
        areasAutorizadas: ["a3", "a4"],
        categoriaId: "c2",
        tipoId: "t2",
        version: "2.1",
        fechaPublicacion: "2026-07-22",
        estado: "publicado",
        publicadoPor: "Diego Peñaranda",
        consultas: 97,
        versiones: [
            { numero: "2.1", fecha: "2026-07-22", autor: "Diego Peñaranda", notas: "Nuevos canales de reporte." },
            { numero: "2.0", fecha: "2026-02-09", autor: "Diego Peñaranda", notas: "Reestructuración por niveles." },
        ],
    },
    {
        id: "d6",
        nombre: "Programa de bienestar laboral 2026",
        descripcion: "Actividades de bienestar, cronograma y responsables para la vigencia 2026.",
        archivo: "programa-bienestar-2026.pdf",
        areaId: "a1",
        visibleTodas: true,
        areasAutorizadas: [],
        categoriaId: "c1",
        tipoId: "t3",
        version: "1.0",
        fechaPublicacion: "2026-07-25",
        estado: "publicado",
        publicadoPor: "Laura Restrepo",
        consultas: 156,
        versiones: [{ numero: "1.0", fecha: "2026-07-25", autor: "Laura Restrepo", notas: "Versión inicial." }],
    },
    {
        id: "d7",
        nombre: "Manual de ejecución presupuestal",
        descripcion: "Criterios y controles para la ejecución del presupuesto anual.",
        archivo: "manual-presupuesto-v4.pdf",
        areaId: "a2",
        visibleTodas: false,
        areasAutorizadas: ["a2", "a4"],
        categoriaId: "c3",
        tipoId: "t4",
        version: "4.0",
        fechaPublicacion: "2026-05-30",
        estado: "publicado",
        publicadoPor: "Ana Gutiérrez",
        consultas: 210,
        versiones: [
            { numero: "4.0", fecha: "2026-05-30", autor: "Ana Gutiérrez", notas: "Actualización de topes de gasto." },
            { numero: "3.0", fecha: "2025-06-01", autor: "Ana Gutiérrez", notas: "Vigencia anterior." },
        ],
    },
    {
        id: "d8",
        nombre: "Política de teletrabajo",
        descripcion: "Condiciones, requisitos y deberes aplicables a la modalidad de teletrabajo.",
        archivo: "politica-teletrabajo-v1.pdf",
        areaId: "a1",
        visibleTodas: false,
        areasAutorizadas: ["a1"],
        categoriaId: "c5",
        tipoId: "t5",
        version: "0.9",
        fechaPublicacion: "2026-07-27",
        estado: "borrador",
        publicadoPor: "Laura Restrepo",
        consultas: 12,
        versiones: [{ numero: "0.9", fecha: "2026-07-27", autor: "Laura Restrepo", notas: "Borrador en revisión jurídica." }],
    },
    {
        id: "d9",
        nombre: "Procedimiento de auditoría interna",
        descripcion: "Planeación, ejecución y cierre de auditorías internas de calidad.",
        archivo: "procedimiento-auditoria-v2.pdf",
        areaId: "a4",
        visibleTodas: false,
        areasAutorizadas: ["a4", "a2"],
        categoriaId: "c4",
        tipoId: "t6",
        version: "2.0",
        fechaPublicacion: "2026-04-18",
        estado: "publicado",
        publicadoPor: "Jorge Salas",
        consultas: 143,
        versiones: [
            { numero: "2.0", fecha: "2026-04-18", autor: "Jorge Salas", notas: "Se agregan listas de verificación." },
            { numero: "1.0", fecha: "2025-04-10", autor: "Jorge Salas", notas: "Versión inicial." },
        ],
    },
    {
        id: "d10",
        nombre: "Plantilla de informe de gestión",
        descripcion: "Formato para la entrega trimestral de informes por área.",
        archivo: "plantilla-informe-v2.docx",
        areaId: "a4",
        visibleTodas: false,
        areasAutorizadas: ["a1", "a3"],
        categoriaId: "c4",
        tipoId: "t1",
        version: "2.0",
        fechaPublicacion: "2026-03-14",
        estado: "inactivo",
        publicadoPor: "Jorge Salas",
        consultas: 64,
        versiones: [{ numero: "2.0", fecha: "2026-03-14", autor: "Jorge Salas", notas: "Reemplazada por el tablero digital." }],
    },
    {
        id: "d11",
        nombre: "Protocolo de atención al usuario interno",
        descripcion: "Estándares de atención, tiempos de respuesta y escalamiento de solicitudes.",
        archivo: "protocolo-atencion-v1.pdf",
        areaId: "a2",
        visibleTodas: false,
        areasAutorizadas: ["a2"],
        categoriaId: "c5",
        tipoId: "t2",
        version: "1.1",
        fechaPublicacion: "2026-07-05",
        estado: "publicado",
        publicadoPor: "Ana Gutiérrez",
        consultas: 88,
        versiones: [
            { numero: "1.1", fecha: "2026-07-05", autor: "Ana Gutiérrez", notas: "Ajuste de tiempos de respuesta." },
            { numero: "1.0", fecha: "2025-11-02", autor: "Ana Gutiérrez", notas: "Versión inicial." },
        ],
    },
    {
        id: "d12",
        nombre: "Programa de capacitación técnica",
        descripcion: "Rutas de formación técnica para equipos de tecnología y operaciones.",
        archivo: "programa-capacitacion-v1.pdf",
        areaId: "a3",
        visibleTodas: false,
        areasAutorizadas: ["a3", "a1"],
        categoriaId: "c1",
        tipoId: "t3",
        version: "1.0",
        fechaPublicacion: "2026-06-20",
        estado: "publicado",
        publicadoPor: "Diego Peñaranda",
        consultas: 121,
        versiones: [{ numero: "1.0", fecha: "2026-06-20", autor: "Diego Peñaranda", notas: "Versión inicial." }],
    },
];

export const notificacionesIniciales: Notificacion[] = [
    {
        id: "n1",
        tipo: "nuevo",
        titulo: "Nuevo documento publicado",
        mensaje: "Programa de bienestar laboral 2026 fue publicado por Talento Humano.",
        fecha: "2026-07-25 09:14",
        leida: false,
        documentoId: "d6",
    },
    {
        id: "n2",
        tipo: "version",
        titulo: "Nueva versión disponible",
        mensaje: "Manual de inducción y reinducción pasó a la versión 3.0.",
        fecha: "2026-07-18 15:40",
        leida: false,
        documentoId: "d1",
    },
    {
        id: "n3",
        tipo: "version",
        titulo: "Nueva versión disponible",
        mensaje: "Protocolo de respuesta ante incidentes de seguridad pasó a la versión 2.1.",
        fecha: "2026-07-22 11:05",
        leida: false,
        documentoId: "d5",
    },
    {
        id: "n4",
        tipo: "version",
        titulo: "Nueva versión disponible",
        mensaje: "Procedimiento de solicitud de vacaciones pasó a la versión 1.2.",
        fecha: "2026-06-28 08:22",
        leida: true,
        documentoId: "d3",
    },
    {
        id: "n5",
        tipo: "nuevo",
        titulo: "Nuevo documento publicado",
        mensaje: "Plantilla de acta de reunión ya está disponible en la biblioteca.",
        fecha: "2026-06-12 16:31",
        leida: true,
        documentoId: "d4",
    },
];

export const actividadInicial: Actividad[] = [
    { id: "ac1", usuario: "Laura Restrepo", accion: "Publicó documento", detalle: "Programa de bienestar laboral 2026", fecha: "2026-07-25 09:14" },
    { id: "ac2", usuario: "Diego Peñaranda", accion: "Publicó nueva versión", detalle: "Protocolo de respuesta ante incidentes v2.1", fecha: "2026-07-22 11:05" },
    { id: "ac3", usuario: "Laura Restrepo", accion: "Creó usuario", detalle: "jorge.salas@empresa.com", fecha: "2026-07-20 10:02" },
    { id: "ac4", usuario: "Diego Peñaranda", accion: "Actualizó categoría", detalle: "Seguridad de la información", fecha: "2026-07-19 17:45" },
    { id: "ac5", usuario: "Laura Restrepo", accion: "Publicó nueva versión", detalle: "Manual de inducción v3.0", fecha: "2026-07-18 15:40" },
    { id: "ac6", usuario: "Jorge Salas", accion: "Cambió estado", detalle: "Plantilla de informe de gestión → inactivo", fecha: "2026-07-14 09:00" },
];
