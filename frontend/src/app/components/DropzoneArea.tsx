import { Upload, X } from "lucide-react";
import { useCallback } from "react";
import { useDropzone, type FileRejection } from "react-dropzone";

import { Button } from "@/components/ui/button";
import {
    extensionProhibida,
    mensajeExtensionProhibida,
    MENSAJE_LIMITE_MB,
} from "@/lib/validacion-archivo";
import { cn } from "@/lib/utils";

export interface DropzoneAreaProps {
    archivo: File | null;
    disabled?: boolean;
    error?: string;
    maxSizeBytes: number;
    onArchivoSeleccionado: (file: File) => void;
    onQuitarArchivo?: () => void;
    onRechazo?: (mensaje: string) => void;
}

function mensajeRechazo(rechazos: FileRejection[]): string {
    for (const rechazo of rechazos) {
        for (const error of rechazo.errors) {
            if (error.code === "file-too-large") {
                return MENSAJE_LIMITE_MB;
            }
            if (error.code === "file-invalid-type" && error.message) {
                return error.message;
            }
        }
    }
    return "No se pudo cargar el archivo seleccionado.";
}

export function DropzoneArea({
    archivo,
    disabled = false,
    error,
    maxSizeBytes,
    onArchivoSeleccionado,
    onQuitarArchivo,
    onRechazo,
}: DropzoneAreaProps) {
    const validarArchivo = useCallback((file: File) => {
        const extension = extensionProhibida(file.name);
        if (extension) {
            return {
                code: "file-invalid-type",
                message: mensajeExtensionProhibida(extension),
            };
        }
        return null;
    }, []);

    const onDropAccepted = useCallback(
        (acceptedFiles: File[]) => {
            const file = acceptedFiles[0];
            if (file) {
                onArchivoSeleccionado(file);
            }
        },
        [onArchivoSeleccionado],
    );

    const onDropRejected = useCallback(
        (rejectedFiles: FileRejection[]) => {
            if (rejectedFiles.length > 0) {
                onRechazo?.(mensajeRechazo(rejectedFiles));
            }
        },
        [onRechazo],
    );

    const { getRootProps, getInputProps, isDragActive, isDragReject, open } = useDropzone({
        onDropAccepted,
        onDropRejected,
        disabled,
        multiple: false,
        maxSize: maxSizeBytes,
        validator: validarArchivo,
        noClick: archivo != null,
        noKeyboard: false,
    });

    const archivoValido = archivo != null && !error;
    const arrastrandoValido = isDragActive && !isDragReject;
    const arrastrandoInvalido = isDragActive && isDragReject;

    return (
        <div
            {...getRootProps({
                className: cn(
                    "rounded-md border-2 border-dashed transition-colors outline-none",
                    archivoValido
                        ? "border-emerald-500 bg-emerald-500/5"
                        : error
                          ? "border-destructive bg-destructive/5"
                          : arrastrandoValido
                            ? "border-primary bg-primary/5"
                            : arrastrandoInvalido
                              ? "border-destructive bg-destructive/5"
                              : "border-border bg-white hover:border-primary/40 hover:bg-slate-50/80",
                    disabled && "cursor-not-allowed opacity-50",
                    !disabled && !archivo && "cursor-pointer",
                ),
            })}
            aria-invalid={error ? true : undefined}
        >
            <input {...getInputProps()} aria-label="Seleccionar archivo del documento" />

            {archivoValido && archivo ? (
                <div className="flex flex-col gap-2 p-4 sm:flex-row sm:items-center sm:justify-between">
                    <div className="min-w-0 space-y-0.5">
                        <p className="text-sm font-medium text-emerald-700">Archivo seleccionado</p>
                        <p className="truncate text-sm font-medium text-slate-900">{archivo.name}</p>
                        <p className="text-xs text-emerald-800">
                            {(archivo.size / (1024 * 1024)).toFixed(2)} MB
                        </p>
                    </div>
                    <div className="flex shrink-0 gap-2">
                        <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            className="!bg-white !text-slate-900"
                            disabled={disabled}
                            onClick={(evento) => {
                                evento.stopPropagation();
                                open();
                            }}
                        >
                            Reemplazar
                        </Button>
                        {onQuitarArchivo ? (
                            <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                disabled={disabled}
                                onClick={(evento) => {
                                    evento.stopPropagation();
                                    onQuitarArchivo();
                                }}
                                aria-label="Quitar archivo"
                            >
                                <X className="size-4" />
                            </Button>
                        ) : null}
                    </div>
                </div>
            ) : (
                <div className="flex flex-col items-center gap-2 px-4 py-8 text-center">
                    <Upload
                        className={cn(
                            "size-8",
                            arrastrandoValido
                                ? "text-primary"
                                : arrastrandoInvalido
                                  ? "text-destructive"
                                  : "text-muted-foreground",
                        )}
                        aria-hidden
                    />
                    <p className="text-sm font-medium text-slate-900">
                        {arrastrandoValido
                            ? "Suelta el archivo aquí"
                            : arrastrandoInvalido
                              ? "Archivo no válido"
                              : "Arrastra y suelta un archivo aquí o haz clic para seleccionarlo"}
                    </p>
                    {!arrastrandoValido && !arrastrandoInvalido ? (
                        <p className="text-xs text-muted-foreground">
                            Tamaño máximo: 10 MB. No se permiten archivos APK ni TXT.
                        </p>
                    ) : null}
                </div>
            )}
        </div>
    );
}
