import { tiposIniciales } from "@/lib/data";

/* =======================================================
 * CÓDIGO INSTITUCIONAL Y REGEX
 * ======================================================= */

/**
 * Estructura:
 * CLASIFICACIÓN - SUBPROCESO - TIPO - CONSECUTIVO
 * Ejemplo: PR-L&D-IN-03
 */
const REGEX_CODIGO = /^[A-Z0-9&_]+-[A-Z0-9&_]+-[A-Z0-9&_]+-[A-Z0-9&_]+$/i;


/* =======================================================
 * ABREVIATURAS DE SUBPROCESOS
 * ======================================================= */

const ABREVIATURAS_SUBPROCESOS: Record<
    string,
    { subProcesoId: string; areaId: string }
> = {
    // Área a1: Gestión Humana
    "C&D": { subProcesoId: "c1", areaId: "a1" },
    CD: { subProcesoId: "c1", areaId: "a1" },
    GHM: { subProcesoId: "c2", areaId: "a1" },
    SST: { subProcesoId: "c3", areaId: "a1" },
    SQC: { subProcesoId: "c4", areaId: "a1" },
    RCI: { subProcesoId: "c5", areaId: "a1" },

    // Área a2: Gestión Ambiental
    CAP: { subProcesoId: "c6", areaId: "a2" },
    CPL: { subProcesoId: "c7", areaId: "a2" },
    RLL: { subProcesoId: "c8", areaId: "a2" },
    CRS: { subProcesoId: "c9", areaId: "a2" },
    "L&D": { subProcesoId: "c10", areaId: "a2" },
    LD: { subProcesoId: "c10", areaId: "a2" },
    AMB: { subProcesoId: "c11", areaId: "a2" },

    // Área a3: Gestión de Calidad
    AUD: { subProcesoId: "c12", areaId: "a3" },
    BPH: { subProcesoId: "c13", areaId: "a3" },
    PCA: { subProcesoId: "c14", areaId: "a3" },
    GDO: { subProcesoId: "c15", areaId: "a3" },
    GDC: { subProcesoId: "c16", areaId: "a3" },
    MEX: { subProcesoId: "c17", areaId: "a3" },
    PDM: { subProcesoId: "c18", areaId: "a3" },
    REC: { subProcesoId: "c19", areaId: "a3" },
    SIG: { subProcesoId: "c20", areaId: "a3" },
    TZR: { subProcesoId: "c21", areaId: "a3" },
    PNC: { subProcesoId: "c22", areaId: "a3" },
    DEI: { subProcesoId: "c23", areaId: "a3" },
    MCO: { subProcesoId: "c24", areaId: "a3" },
    PQR: { subProcesoId: "c40" ,areaId: "a3" },

    // Área a4: Gestión Logística
    ABT: { subProcesoId: "c25", areaId: "a4" },
    LOG: { subProcesoId: "c26", areaId: "a4" },
    TRS: { subProcesoId: "c27", areaId: "a4" },
    ALM: { subProcesoId: "c28", areaId: "a4" },

    // Área a5: Gestión Comercial
    GCV: { subProcesoId: "c29", areaId: "a5" },

    // Área a6: Gestión de Producción
    GPR: { subProcesoId: "c30", areaId: "a6" },

    // Área a7: Gestión de Mantenimiento
    CAL: { subProcesoId: "c31", areaId: "a7" },
    MME: { subProcesoId: "c32", areaId: "a7" },
    MEI: { subProcesoId: "c33", areaId: "a7" },

    // Área a8: Gestión de Seguridad Física
    SVG: { subProcesoId: "c34", areaId: "a8" },

    // Área a9: Gestión TICs
    TIC: { subProcesoId: "c35", areaId: "a9" },

    // Área a10: Gestión Financiera
    GAD: { subProcesoId: "c36", areaId: "a10" },
    GCF: { subProcesoId: "c37", areaId: "a10" },

    // Área a11: Gestión de Compras
    CPR: { subProcesoId: "c38", areaId: "a11" },
    GCO: { subProcesoId: "c39", areaId: "a11" },
};


/* =======================================================
 * ABREVIATURAS DE TIPOS DE DOCUMENTO
 * ======================================================= */

const ABREVIATURAS_TIPOS_DOCUMENTO: Record<string, string> = {
    PG: "Programa",
    MA: "Manual",
    PC: "Proceso",
    PD: "Procedimiento",
    PL: "Política",
    RT: "Reglamento",
    CR: "Caracterización",
    IN: "Instructivo",
    PT: "Protocolo",
    FO: "Formato",
    FT: "Ficha Técnica",
    DG: "Diagrama",
    OD: "Otros Documentos",
};


/* =======================================================
 * RESULTADO
 * ======================================================= */

export interface ResultadoExtraccionDocumento {
    valido: boolean;
    codigo: string | null;
    nombre: string;
    version: string | null;
    mensaje: string | null;
    error?: string;
    clasificacion: string | null;
    areaId: string | null;
    subProcesoId: string | null;
    tipoId: string | null;
}


/* =======================================================
 * ESTRUCTURAS INTERNAS
 * ======================================================= */

interface CodigoExtraido {
    codigo: string | null;
    clasificacion: string | null;
    abreviaturaSubProceso: string | null;
    abreviaturaTipo: string | null;
    consecutivo: string | null;
    resto: string;
}


interface VersionExtraida {
    version: string | null;
    nombre: string;
}


/* =======================================================
 * PUNTO DE ENTRADA PRINCIPAL
 * ======================================================= */

export function extraerInformacionDocumento(
    nombreArchivo: string,
): ResultadoExtraccionDocumento {

    /* ---------------------------------------------------
     * Quitar extensión
     * --------------------------------------------------- */

    const sinExtension =
        quitarExtension(nombreArchivo);


    /* ---------------------------------------------------
     * Normalizar espacios
     * --------------------------------------------------- */

    const limpio =
        normalizarEspacios(sinExtension);


    /* ---------------------------------------------------
     * Extraer código y nombre
     * --------------------------------------------------- */

    const {
        codigo,
        clasificacion,
        abreviaturaSubProceso,
        abreviaturaTipo,
        resto,
    } = dividirCodigoYNombre(limpio);


    /* ---------------------------------------------------
     * Validar código
     * --------------------------------------------------- */

    if (
        !codigo ||
        !validarCodigo(codigo)
    ) {

        return crearResultadoInvalido(
            "El archivo no cumple la nomenclatura institucional. Ingrese el código manualmente o agréguelo en el nombre del archivo.",
        );

    }


    /* ---------------------------------------------------
     * Buscar subproceso
     * --------------------------------------------------- */

    const subProceso =
        obtenerSubProcesoPorAbreviatura(
            abreviaturaSubProceso,
        );


    if (!subProceso) {

        return crearResultadoInvalido(
            `No se encontró un subproceso asociado a la abreviatura "${abreviaturaSubProceso}".`,
        );

    }


    /* ---------------------------------------------------
     * Buscar tipo de documento
     * --------------------------------------------------- */

    const tipoDocumento =
        obtenerTipoPorAbreviatura(
            abreviaturaTipo,
        );


    if (!tipoDocumento) {

        return crearResultadoInvalido(
            `No se encontró un tipo de documento asociado a la abreviatura "${abreviaturaTipo}".`,
        );

    }


    /* ---------------------------------------------------
     * Extraer versión
     * --------------------------------------------------- */

    const {
        version,
        nombre,
    } = extraerVersion(resto);


    /* ---------------------------------------------------
     * Resultado final
     * --------------------------------------------------- */

    return crearResultadoValido(
        codigo,
        nombre,
        version,
        clasificacion,
        subProceso.areaId,
        subProceso.subProcesoId,
        tipoDocumento.id,
    );
}


/* =======================================================
 * SUBPROCESO
 * ======================================================= */

function obtenerSubProcesoPorAbreviatura(
    abreviatura: string | null,
): {
    subProcesoId: string;
    areaId: string;
} | null {

    if (!abreviatura) {
        return null;
    }


    const abreviaturaNormalizada =
        abreviatura
            .trim()
            .toUpperCase();


    return (
        ABREVIATURAS_SUBPROCESOS[
            abreviaturaNormalizada
            ] ?? null
    );
}


/* =======================================================
 * LIMPIEZA
 * ======================================================= */

function quitarExtension(
    nombreArchivo: string,
): string {

    const indice =
        nombreArchivo.lastIndexOf(".");


    if (indice === -1) {
        return nombreArchivo;
    }


    return nombreArchivo.substring(
        0,
        indice,
    );
}


function normalizarEspacios(
    texto: string,
): string {

    return texto
        .trim()
        .replace(/\s+/g, " ");
}


/* =======================================================
 * EXTRACCIÓN DEL CÓDIGO
 * ======================================================= */

function dividirCodigoYNombre(
    texto: string,
): CodigoExtraido {

    let codigo: string;
    let resto: string;

    const indicePrimerEspacio =
        texto.indexOf(" ");


    if (indicePrimerEspacio === -1) {
        codigo = texto.trim();
        resto = "";
    } else {
        codigo = texto.substring(0, indicePrimerEspacio).trim();
        resto = texto.substring(indicePrimerEspacio + 1).trim();
    }

    // Limpiar guiones o separadores sobrantes al inicio de resto (ej. "- Nombre")
    resto = resto.replace(/^[-_–—\s]+/, "").trim();

    /*
     * Separación:
     * PR-L&D-IN-03
     * 0 → PR
     * 1 → L&D
     * 2 → IN
     * 3 → 03
     */

    const partes =
        codigo.split("-");


    if (partes.length !== 4) {

        return {
            codigo: null,
            clasificacion: null,
            abreviaturaSubProceso: null,
            abreviaturaTipo: null,
            consecutivo: null,
            resto,
        };
    }


    const [
        clasificacion,
        abreviaturaSubProceso,
        abreviaturaTipo,
        consecutivo,
    ] = partes;


    return {
        codigo,
        clasificacion,
        abreviaturaSubProceso,
        abreviaturaTipo,
        consecutivo,
        resto,
    };
}


/* =======================================================
 * VALIDACIÓN DEL CÓDIGO
 * ======================================================= */

function validarCodigo(
    codigo: string,
): boolean {

    return REGEX_CODIGO.test(
        codigo.toUpperCase(),
    );
}


/* =======================================================
 * TIPO DE DOCUMENTO
 * ======================================================= */

function obtenerTipoPorAbreviatura(
    abreviatura: string | null,
) {

    if (!abreviatura) {
        return null;
    }


    const abreviaturaNormalizada =
        abreviatura
            .trim()
            .toUpperCase();


    const nombreTipo =
        ABREVIATURAS_TIPOS_DOCUMENTO[
            abreviaturaNormalizada
            ];


    if (!nombreTipo) {
        return null;
    }


    return (
        tiposIniciales.find(
            (tipo) =>
                tipo.nombre === nombreTipo,
        ) ?? null
    );
}


/* =======================================================
 * VERSIÓN
 * ======================================================= */

function extraerVersion(
    texto: string,
): VersionExtraida {

    if (!texto) {
        return {
            version: null,
            nombre: "",
        };
    }

    /*
     * Detecta:
     * V1
     * V1.0
     * V2
     * V2.1
     * Únicamente cuando aparece al final.
     */

    const patron =
        /\s+[Vv](\d+(?:\.\d+)?)$/;


    const coincidencia =
        texto.match(patron);


    if (!coincidencia) {

        return {
            version: null,
            nombre: texto.trim(),
        };
    }


    const version =
        coincidencia[1];


    const nombre =
        texto
            .replace(
                patron,
                "",
            )
            .trim();


    return {
        version,
        nombre,
    };
}


/* =======================================================
 * RESULTADO VÁLIDO
 * ======================================================= */

function crearResultadoValido(
    codigo: string,
    nombre: string,
    version: string | null,
    clasificacion: string | null,
    areaId: string | null,
    subProcesoId: string | null,
    tipoId: string | null,
): ResultadoExtraccionDocumento {

    return {
        valido: true,
        codigo,
        nombre,
        version,
        mensaje: null,
        clasificacion,
        areaId,
        subProcesoId,
        tipoId,
    };
}


/* =======================================================
 * RESULTADO INVÁLIDO
 * ======================================================= */

function crearResultadoInvalido(
    mensaje: string,
): ResultadoExtraccionDocumento {

    return {
        valido: false,
        codigo: null,
        nombre: "",
        version: null,
        mensaje,
        clasificacion: null,
        areaId: null,
        subProcesoId: null,
        tipoId: null,
    };
}