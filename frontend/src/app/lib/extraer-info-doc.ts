const REGEX_CODIGO = /^[A-Z]{1,3}(?:-[A-Z&]{1,3})*(?:-[A-Z]{1,2})?-\d+$/;//const REGEX_VERSION =version;

export interface ResultadoExtraccionDocumento {
    valido: boolean;
    codigo: string | null;
    nombre: string;
    version: string | null;
    mensaje: string | null;
    error?: string;
}

interface CodigoExtraido {
    codigo: string | null;
    resto: string;
}

interface VersionExtraida {
    version: string | null;
    nombre: string;
}

/**
 * Punto de entrada principal.
 */
export function extraerInformacionDocumento(
    nombreArchivo: string,
): ResultadoExtraccionDocumento {

    const sinExtension = quitarExtension(nombreArchivo);

    const limpio = normalizarEspacios(sinExtension);

    const {
        codigo,
        resto,
    } = dividirCodigoYNombre(limpio);

    if (!codigo || !validarCodigo(codigo)) {

        return crearResultadoInvalido(
            "El archivo no cumple la nomenclatura institucional. Ingrese el código manualmente o agréguelo en el nombre del archivo."
        );

    }

    const {
        version,
        nombre,
    } = extraerVersion(resto);

    return crearResultadoValido(
        codigo,
        nombre,
        version,
    );
}

/* =======================================================
 * LIMPIEZA
 * ======================================================= */

function quitarExtension(nombreArchivo: string): string {

    const indice = nombreArchivo.lastIndexOf(".");

    if (indice === -1) {
        return nombreArchivo;
    }

    return nombreArchivo.substring(0, indice);

}

function normalizarEspacios(texto: string): string {

    return texto
        .trim()
        .replace(/\s+/g, " ");

}

/* =======================================================
 * CÓDIGO
 * ======================================================= */

function dividirCodigoYNombre(
    texto: string,
): CodigoExtraido {

    const indicePrimerEspacio = texto.indexOf(" ");

    if (indicePrimerEspacio === -1) {

        return {
            codigo: null,
            resto: "",
        };

    }

    const codigo = texto
        .substring(0, indicePrimerEspacio)
        .trim();

    const resto = texto
        .substring(indicePrimerEspacio + 1)
        .trim();

    return {
        codigo,
        resto,
    };

}

function validarCodigo(
    codigo: string,
): boolean {

    return REGEX_CODIGO.test(codigo);

}

/* =======================================================
 * VERSIÓN
 * ======================================================= */

function extraerVersion(
    texto: string,
): VersionExtraida {

    const patron = /\s+[Vv](\d+(?:\.\d+)?)$/;

    const coincidencia = texto.match(patron);

    if (!coincidencia) {

        return {
            version: null,
            nombre: texto.trim(),
        };

    }

    const version = coincidencia[1];

    const nombre = texto
        .replace(patron, "")
        .trim();

    return {
        version,
        nombre,
    };

}

/* =======================================================
 * RESULTADOS
 * ======================================================= */

function crearResultadoValido(
    codigo: string,
    nombre: string,
    version: string | null,
): ResultadoExtraccionDocumento {

    return {
        valido: true,
        codigo,
        nombre,
        version,
        mensaje: null,
    };

}

function crearResultadoInvalido(
    mensaje: string,
): ResultadoExtraccionDocumento {

    return {
        valido: false,
        codigo: null,
        nombre: "",
        version: null,
        mensaje,
    };

}


