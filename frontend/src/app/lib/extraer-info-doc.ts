import { normalizarNombre } from "./normalizar-nombre";

export type VersionDetectada =
    | { tipo: "entera"; valor: number }
    | { tipo: "decimal"; valor: string }
    | { tipo: "ninguna" };

export interface MetadatosExtraidosDocumento {
    reconocido: boolean;
    codigo: string | null;
    clasificacion: string | null;
    abreviaturaSubproceso: string | null;
    nombreSubproceso: string | null;
    abreviaturaTipo: string | null;
    nombreTipo: string | null;
    nombre: string;
    version: VersionDetectada;
}

interface CodigoInstitucional {
    codigo: string;
    clasificacion: string;
    abreviaturaSubproceso: string;
    abreviaturaTipo: string;
}

const NOMBRES_SUBPROCESO_POR_ABREVIATURA: Record<string, string> = {
    "c&d": "Capacitación y Desarrollo",
    cd: "Capacitación y Desarrollo",
    ghm: "Gestión Humana",
    sst: "Salud y Seguridad en el Trabajo",
    sqc: "Sustancias Químicas",
    rci: "Programa de Respeto, Convivencia e Inclusión",
    cap: "Calidad del Agua Potable",
    cpl: "Control de Plagas",
    rll: "Control de Residuos Líquidos",
    crs: "Control de Residuos Sólidos",
    "l&d": "Limpieza y Desinfección",
    ld: "Limpieza y Desinfección",
    amb: "Gestión Ambiental",
    aud: "Auditoría Interna",
    bph: "Buenas Prácticas Higiénicas",
    pca: "Control de Alérgenos",
    gdo: "Gestión Documental",
    gdc: "Gestión de la Calidad",
    mex: "Material Extraño",
    pdm: "Plan de Muestreo",
    rec: "Recall",
    sig: "Sistemas Integrados de Gestión",
    tzr: "Trazabilidad",
    pnc: "Programa de Producto No Conforme",
    dei: "Desarrollo e Innovación",
    mco: "Proceso de Mejora Continua",
    pqr: "Programa de Peticiones Quejas y Reclamos",
    abt: "Almacén y Abastecimiento de Insumos",
    log: "Logística",
    trs: "Transporte",
    alm: "Programa de Almacenamiento",
    gcv: "Gestión Comercial y Ventas",
    gpr: "Gestión de la Producción",
    cal: "Calibración y Verificación de Equipos de Medición",
    mme: "Mantenimiento de Maquinaria y Equipos",
    mei: "Mantenimiento de Edificios e Instalaciones",
    svg: "Seguridad y Vigilancia",
    tic: "Tecnología Informática y de Comunicaciones",
    gad: "Gestión Administrativa",
    gcf: "Gestión Contable y Financiera",
    cpr: "Control de Proveedores",
    gco: "Gestión de Compras",
};

const NOMBRES_TIPO_POR_ABREVIATURA: Record<string, string> = {
    ma: "Manual",
    pg: "Programa",
    pc: "Proceso",
    pd: "Procedimiento",
    pl: "Política",
    rt: "Reglamento",
    cr: "Caracterización",
    in: "Instructivo",
    pt: "Protocolo",
    fo: "Formato",
    ft: "Ficha Técnica",
    dg: "Diagrama",
    od: "Otros Documentos",
};

const RESULTADO_VACIO: MetadatosExtraidosDocumento = {
    reconocido: false,
    codigo: null,
    clasificacion: null,
    abreviaturaSubproceso: null,
    nombreSubproceso: null,
    abreviaturaTipo: null,
    nombreTipo: null,
    nombre: "",
    version: { tipo: "ninguna" },
};

export function extraerMetadatosDesdeArchivo(
    nombreArchivo: string,
): MetadatosExtraidosDocumento {
    const sinExtension = quitarExtension(nombreArchivo);
    const limpio = normalizarEspacios(sinExtension);
    const { codigoTexto, resto } = dividirCodigoYNombre(limpio);

    if (!codigoTexto) {
        return { ...RESULTADO_VACIO };
    }

    const codigoInstitucional = parsearCodigoInstitucional(codigoTexto);
    if (!codigoInstitucional) {
        return { ...RESULTADO_VACIO };
    }

    const nombreSubproceso = resolverNombreSubproceso(
        codigoInstitucional.abreviaturaSubproceso,
    );
    const nombreTipo = resolverNombreTipo(codigoInstitucional.abreviaturaTipo);
    const { version, nombre } = extraerVersionYNombre(resto);

    return {
        reconocido: true,
        codigo: codigoInstitucional.codigo,
        clasificacion: codigoInstitucional.clasificacion,
        abreviaturaSubproceso: codigoInstitucional.abreviaturaSubproceso,
        nombreSubproceso,
        abreviaturaTipo: codigoInstitucional.abreviaturaTipo,
        nombreTipo,
        nombre,
        version,
    };
}

export function resolverNombreSubproceso(abreviatura: string): string | null {
    const clave = normalizarAbreviatura(abreviatura);
    return NOMBRES_SUBPROCESO_POR_ABREVIATURA[clave] ?? null;
}

export function resolverNombreTipo(abreviatura: string): string | null {
    const clave = normalizarAbreviatura(abreviatura);
    return NOMBRES_TIPO_POR_ABREVIATURA[clave] ?? null;
}

export function nombresCoinciden(a: string, b: string): boolean {
    return normalizarNombre(a) === normalizarNombre(b);
}

function quitarExtension(nombreArchivo: string): string {
    const indice = nombreArchivo.lastIndexOf(".");
    if (indice === -1) {
        return nombreArchivo;
    }
    return nombreArchivo.substring(0, indice);
}

function normalizarEspacios(texto: string): string {
    return texto.trim().replace(/\s+/g, " ");
}

function dividirCodigoYNombre(texto: string): { codigoTexto: string | null; resto: string } {
    const indicePrimerEspacio = texto.indexOf(" ");
    if (indicePrimerEspacio === -1) {
        return { codigoTexto: null, resto: "" };
    }

    return {
        codigoTexto: texto.substring(0, indicePrimerEspacio).trim(),
        resto: texto.substring(indicePrimerEspacio + 1).trim(),
    };
}

function parsearCodigoInstitucional(codigo: string): CodigoInstitucional | null {
    const partes = codigo.split("-");
    if (partes.length < 4) {
        return null;
    }

    const clasificacion = partes[0]?.trim();
    const consecutivo = partes[partes.length - 1]?.trim();
    const abreviaturaTipo = partes[partes.length - 2]?.trim();
    const abreviaturaSubproceso = partes.slice(1, -2).join("-").trim();

    if (!clasificacion || !/^[A-Za-z]{1,3}$/.test(clasificacion)) {
        return null;
    }
    if (!consecutivo || !/^\d+$/.test(consecutivo)) {
        return null;
    }
    if (!abreviaturaTipo || !/^[A-Za-z&]{1,3}$/.test(abreviaturaTipo)) {
        return null;
    }
    if (!abreviaturaSubproceso || !/^[A-Za-z0-9&]+$/.test(abreviaturaSubproceso)) {
        return null;
    }

    return {
        codigo,
        clasificacion: clasificacion.toUpperCase(),
        abreviaturaSubproceso: abreviaturaSubproceso.toUpperCase(),
        abreviaturaTipo: abreviaturaTipo.toUpperCase(),
    };
}

function extraerVersionYNombre(texto: string): { version: VersionDetectada; nombre: string } {
    const patronDecimal = /\s+[Vv](\d+\.\d+)$/;
    const coincidenciaDecimal = texto.match(patronDecimal);
    if (coincidenciaDecimal) {
        return {
            version: { tipo: "decimal", valor: coincidenciaDecimal[1] },
            nombre: texto.replace(patronDecimal, "").trim(),
        };
    }

    const patronEntera = /\s+[Vv](\d+)$/;
    const coincidenciaEntera = texto.match(patronEntera);
    if (coincidenciaEntera) {
        return {
            version: { tipo: "entera", valor: Number.parseInt(coincidenciaEntera[1], 10) },
            nombre: texto.replace(patronEntera, "").trim(),
        };
    }

    return {
        version: { tipo: "ninguna" },
        nombre: texto.trim(),
    };
}

function normalizarAbreviatura(abreviatura: string): string {
    return abreviatura.trim().toLowerCase();
}
