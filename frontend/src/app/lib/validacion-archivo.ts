export const LIMITE_ARCHIVO_BYTES = 10 * 1024 * 1024;

export const MENSAJE_LIMITE_MB = "El archivo no puede superar los 10 MB.";
export const MENSAJE_TIPO_NO_PERMITIDO =
    "Solo se permiten archivos PDF, DOC, DOCX, XLS y XLSX.";

const EXTENSIONES_PERMITIDAS = [".pdf", ".doc", ".docx", ".xls", ".xlsx"] as const;

export type ExtensionPermitida = (typeof EXTENSIONES_PERMITIDAS)[number];

export function extensionArchivo(nombre: string): string | null {
    const normalizado = nombre.replaceAll("\\", "/");
    const nombreBase = normalizado.slice(normalizado.lastIndexOf("/") + 1);
    const indicePunto = nombreBase.lastIndexOf(".");
    if (indicePunto <= 0 || indicePunto === nombreBase.length - 1) {
        return null;
    }
    const extension = nombreBase.slice(indicePunto).toLowerCase();
    if (!/^\.[a-z0-9]+$/.test(extension)) {
        return null;
    }
    return extension;
}

export function extensionPermitida(nombre: string): boolean {
    const extension = extensionArchivo(nombre);
    return extension !== null && (EXTENSIONES_PERMITIDAS as readonly string[]).includes(extension);
}

export function validarArchivoSubida(archivo: File): string | null {
    if (archivo.size > LIMITE_ARCHIVO_BYTES) {
        return MENSAJE_LIMITE_MB;
    }
    if (!extensionPermitida(archivo.name)) {
        return MENSAJE_TIPO_NO_PERMITIDO;
    }
    return null;
}
