export const LIMITE_ARCHIVO_BYTES = 10 * 1024 * 1024;

export const MENSAJE_LIMITE_MB = "El archivo no puede superar los 10 MB.";
export const MENSAJE_APK = "No se permiten archivos APK.";
export const MENSAJE_TXT = "No se permiten archivos TXT.";

const EXTENSIONES_PROHIBIDAS = [".apk", ".txt"] as const;

export type ExtensionProhibida = (typeof EXTENSIONES_PROHIBIDAS)[number];

export function extensionProhibida(nombre: string): ExtensionProhibida | null {
    const lower = nombre.toLowerCase();
    for (const extension of EXTENSIONES_PROHIBIDAS) {
        if (lower.endsWith(extension)) {
            return extension;
        }
    }
    return null;
}

export function mensajeExtensionProhibida(extension: ExtensionProhibida): string {
    if (extension === ".apk") return MENSAJE_APK;
    return MENSAJE_TXT;
}

export function validarArchivoSubida(archivo: File): string | null {
    if (archivo.size > LIMITE_ARCHIVO_BYTES) {
        return MENSAJE_LIMITE_MB;
    }
    const extension = extensionProhibida(archivo.name);
    if (extension) {
        return mensajeExtensionProhibida(extension);
    }
    return null;
}
