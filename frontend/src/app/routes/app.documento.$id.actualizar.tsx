import { ArrowLeft } from "lucide-react";
import { Link, createFileRoute, useNavigate, useParams } from "@tanstack/react-router";
import {
    useCallback,
    useEffect,
    useRef,
    useState,
    type ChangeEvent,
    type FormEvent,
} from "react";
import { toast } from "sonner";

import { DocumentoEstadoBadge } from "@/components/documentos-consulta-ui";
import { AppShell } from "@/components/AppShell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { ApiError } from "@/lib/api";
import {
    obtenerDocumento,
    publicarNuevaVersion,
    type DocumentoDetalle,
} from "@/lib/documentos-api";
import { useIntranet } from "@/lib/store";
import { validarArchivoSubida } from "@/lib/validacion-archivo";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/app/documento/$id/actualizar")({
    head: () => ({
        meta: [{ title: "Actualizar documento — Intranet documental" }],
    }),
    component: ActualizarDocumentoPage,
});

function validarFormulario(
    archivo: File | null,
    descripcionCambio: string,
): Record<string, string> {
    const errores: Record<string, string> = {};

    if (!archivo) {
        errores.archivo = "Debes adjuntar un archivo.";
    } else {
        const errorArchivo = validarArchivoSubida(archivo);
        if (errorArchivo) {
            errores.archivo = errorArchivo;
        }
    }

    const descripcion = descripcionCambio.trim();
    if (!descripcion) {
        errores.descripcionCambio = "La descripción de la nueva versión es obligatoria.";
    } else if (descripcion.length > 500) {
        errores.descripcionCambio =
            "La descripción del cambio no puede superar los 500 caracteres.";
    }

    return errores;
}

function ActualizarDocumentoPage() {
    const { id } = useParams({ from: "/app/documento/$id/actualizar" });
    const navigate = useNavigate();
    const { permisos } = useIntranet();

    const [documento, setDocumento] = useState<DocumentoDetalle | null>(null);
    const [cargando, setCargando] = useState(true);
    const [noDisponible, setNoDisponible] = useState(false);
    const [errorCarga, setErrorCarga] = useState<string | null>(null);

    const [descripcionCambio, setDescripcionCambio] = useState("");
    const [archivo, setArchivo] = useState<File | null>(null);
    const [archivoInputKey, setArchivoInputKey] = useState(0);
    const [errores, setErrores] = useState<Record<string, string>>({});
    const [publicando, setPublicando] = useState(false);

    const requestIdRef = useRef(0);

    const cargarDocumento = useCallback(async () => {
        const requestId = ++requestIdRef.current;
        setCargando(true);
        setNoDisponible(false);
        setErrorCarga(null);
        setDocumento(null);

        try {
            const detalle = await obtenerDocumento(id);
            if (requestId !== requestIdRef.current) return;
            setDocumento(detalle);
        } catch (err) {
            if (requestId !== requestIdRef.current) return;
            if (err instanceof ApiError && (err.status === 404 || err.status === 403)) {
                setNoDisponible(true);
                return;
            }
            setErrorCarga(
                err instanceof ApiError
                    ? err.message
                    : "No fue posible cargar el documento.",
            );
        } finally {
            if (requestId === requestIdRef.current) {
                setCargando(false);
            }
        }
    }, [id]);

    useEffect(() => {
        cargarDocumento();
        return () => {
            requestIdRef.current += 1;
        };
    }, [cargarDocumento]);

    const manejarArchivo = (evento: ChangeEvent<HTMLInputElement>) => {
        const seleccionado = evento.target.files?.[0] ?? null;
        if (seleccionado) {
            const errorArchivo = validarArchivoSubida(seleccionado);
            if (errorArchivo) {
                setArchivo(null);
                setErrores((prev) => ({ ...prev, archivo: errorArchivo }));
                setArchivoInputKey((prev) => prev + 1);
                return;
            }
        }
        setArchivo(seleccionado);
        setErrores((prev) => {
            const { archivo: _, ...resto } = prev;
            return resto;
        });
    };

    const enviar = async (evento: FormEvent) => {
        evento.preventDefault();
        if (!documento || documento.estado === "OBSOLETO") return;

        const nuevosErrores = validarFormulario(archivo, descripcionCambio);
        if (Object.keys(nuevosErrores).length > 0) {
            setErrores(nuevosErrores);
            return;
        }

        setErrores({});
        setPublicando(true);
        try {
            await publicarNuevaVersion(id, descripcionCambio.trim(), archivo!);
            toast.success("Nueva versión publicada correctamente.");
            navigate({ to: "/app/documento/$id", params: { id } });
        } catch (err) {
            if (err instanceof ApiError) {
                if (err.errores) {
                    setErrores(err.errores);
                }
                toast.error(err.message);
            } else {
                toast.error("No fue posible publicar la nueva versión.");
            }
        } finally {
            setPublicando(false);
        }
    };

    const cancelar = () => {
        navigate({ to: "/app/documento/$id", params: { id } });
    };

    if (!permisos.actualizarDocumentos) {
        return (
            <AppShell titulo="Actualizar documento">
                <Card>
                    <CardContent className="py-14 text-center text-sm text-muted-foreground">
                        No cuentas con permisos para publicar nuevas versiones.
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    if (cargando) {
        return (
            <AppShell titulo="Actualizar documento">
                <Card>
                    <CardContent className="py-14 text-center text-sm text-muted-foreground">
                        Cargando documento...
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    if (noDisponible) {
        return (
            <AppShell titulo="Documento no disponible">
                <Button asChild variant="ghost" size="sm" className="-ml-2 mb-4 gap-1.5">
                    <Link to="/app/gestion-documentos">
                        <ArrowLeft className="size-4" /> Volver
                    </Link>
                </Button>
                <Card>
                    <CardContent className="py-14 text-center">
                        <p className="text-sm font-medium">Documento no disponible</p>
                        <p className="mt-2 text-sm text-muted-foreground">
                            El documento no existe o no está disponible para tu usuario.
                        </p>
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    if (errorCarga || !documento) {
        return (
            <AppShell titulo="Actualizar documento">
                <Button asChild variant="ghost" size="sm" className="-ml-2 mb-4 gap-1.5">
                    <Link to="/app/gestion-documentos">
                        <ArrowLeft className="size-4" /> Volver
                    </Link>
                </Button>
                <Card>
                    <CardContent className="space-y-3 py-14 text-center">
                        <p className="text-sm font-medium">{errorCarga ?? "Error inesperado."}</p>
                        <Button variant="outline" size="sm" onClick={cargarDocumento}>
                            Reintentar
                        </Button>
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    const nuevaVersion = documento.numeroVersionActual + 1;
    const esObsoleto = documento.estado === "OBSOLETO";
    const esInactivo = documento.estado === "INACTIVO";
    const formularioDeshabilitado = publicando || esObsoleto;
    const archivoSeleccionadoValido = archivo !== null && !errores.archivo;

    return (
        <AppShell
            titulo="Actualizar documento"
            descripcion="Publica un nuevo archivo sobre el documento existente."
        >
            <div className="mx-auto max-w-3xl space-y-6">
                <Button asChild variant="ghost" size="sm" className="-ml-2 gap-1.5">
                    <Link to="/app/documento/$id" params={{ id }}>
                        <ArrowLeft className="size-4" /> Volver al detalle
                    </Link>
                </Button>

                <Card>
                    <CardHeader>
                        <CardTitle className="text-base">Contexto del documento</CardTitle>
                        <CardDescription>
                            Información de referencia. No se modifica en este flujo.
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        <dl className="grid gap-4 sm:grid-cols-2">
                            <CampoInfo k="Código documental" v={documento.codigo} mono />
                            <CampoInfo k="Documento" v={documento.titulo} />
                            <div>
                                <dt className="text-xs uppercase tracking-wide text-muted-foreground">
                                    Estado
                                </dt>
                                <dd className="mt-1.5">
                                    <DocumentoEstadoBadge estado={documento.estado} />
                                </dd>
                            </div>
                            <CampoInfo
                                k="Versión actual"
                                v={String(documento.numeroVersionActual)}
                            />
                            <CampoInfo k="Nueva versión" v={String(nuevaVersion)} />
                        </dl>
                    </CardContent>
                </Card>

                {esObsoleto ? (
                    <Card>
                        <CardContent className="py-8 text-center">
                            <p className="text-sm font-medium text-amber-800">
                                Este documento está obsoleto y no puede recibir nuevas versiones.
                            </p>
                            <p className="mt-2 text-sm text-muted-foreground">
                                Actívalo nuevamente desde Gestión de documentos para poder
                                actualizarlo.
                            </p>
                            <Button asChild variant="outline" className="mt-4">
                                <Link to="/app/gestion-documentos">Ir a Gestión de documentos</Link>
                            </Button>
                        </CardContent>
                    </Card>
                ) : (
                    <form onSubmit={(e) => void enviar(e)} noValidate>
                        <Card>
                            <CardHeader>
                                <CardTitle className="text-base">Nueva versión</CardTitle>
                                {esInactivo ? (
                                    <CardDescription>
                                        La nueva versión se publicará manteniendo el documento
                                        inactivo.
                                    </CardDescription>
                                ) : null}
                            </CardHeader>
                            <CardContent className="space-y-5">
                                <div className="space-y-1.5">
                                    <Label htmlFor="archivo-nueva-version">
                                        Archivo nuevo <span className="text-destructive">*</span>
                                    </Label>
                                    <div
                                        className={cn(
                                            "rounded-md border transition-colors",
                                            archivoSeleccionadoValido
                                                ? "border-emerald-500 bg-emerald-500/5"
                                                : errores.archivo
                                                  ? "border-destructive/50 bg-destructive/5"
                                                  : "border-border bg-white",
                                        )}
                                    >
                                        <input
                                            key={archivoInputKey}
                                            id="archivo-nueva-version"
                                            type="file"
                                            className="sr-only"
                                            onChange={manejarArchivo}
                                            disabled={formularioDeshabilitado}
                                        />
                                        <label
                                            htmlFor={
                                                formularioDeshabilitado
                                                    ? undefined
                                                    : "archivo-nueva-version"
                                            }
                                            className={cn(
                                                "flex cursor-pointer flex-col gap-1.5 rounded-md p-4",
                                                formularioDeshabilitado &&
                                                    "cursor-not-allowed opacity-50",
                                            )}
                                        >
                                            {archivoSeleccionadoValido && archivo ? (
                                                <>
                                                    <span className="text-sm font-medium text-emerald-700">
                                                        Archivo seleccionado
                                                    </span>
                                                    <span className="truncate text-sm font-medium text-slate-900">
                                                        {archivo.name}
                                                    </span>
                                                    <span className="text-xs text-emerald-800">
                                                        {(archivo.size / (1024 * 1024)).toFixed(2)}{" "}
                                                        MB
                                                    </span>
                                                    <span className="text-xs font-medium text-emerald-700 underline">
                                                        Cambiar archivo
                                                    </span>
                                                </>
                                            ) : (
                                                <>
                                                    <span className="text-sm font-medium text-slate-900">
                                                        Seleccionar archivo
                                                    </span>
                                                    <span className="text-xs text-muted-foreground">
                                                        Ningún archivo seleccionado
                                                    </span>
                                                    <span className="text-xs text-muted-foreground">
                                                        Tamaño máximo: 10 MB. No se permiten
                                                        archivos APK ni TXT.
                                                    </span>
                                                </>
                                            )}
                                        </label>
                                    </div>
                                    {errores.archivo ? (
                                        <p className="text-sm text-destructive">{errores.archivo}</p>
                                    ) : null}
                                </div>

                                <div className="space-y-1.5">
                                    <Label htmlFor="descripcionCambio">
                                        Descripción de la nueva versión{" "}
                                        <span className="text-destructive">*</span>
                                    </Label>
                                    <Textarea
                                        id="descripcionCambio"
                                        rows={3}
                                        placeholder="Indique qué cambió en el archivo (por ejemplo: frecuencias, responsables, anexos)."
                                        className="bg-white"
                                        value={descripcionCambio}
                                        onChange={(e) => setDescripcionCambio(e.target.value)}
                                        disabled={formularioDeshabilitado}
                                        aria-invalid={!!errores.descripcionCambio}
                                    />
                                    {errores.descripcionCambio ? (
                                        <p className="text-sm text-destructive">
                                            {errores.descripcionCambio}
                                        </p>
                                    ) : null}
                                </div>

                                <div className="grid gap-4 sm:grid-cols-2">
                                    <div className="space-y-1.5">
                                        <Label htmlFor="versionActualInfo">Versión actual</Label>
                                        <Input
                                            id="versionActualInfo"
                                            value={String(documento.numeroVersionActual)}
                                            readOnly
                                            disabled
                                            className="bg-muted/40"
                                        />
                                    </div>
                                    <div className="space-y-1.5">
                                        <Label htmlFor="nuevaVersionInfo">Nueva versión</Label>
                                        <Input
                                            id="nuevaVersionInfo"
                                            value={String(nuevaVersion)}
                                            readOnly
                                            disabled
                                            className="bg-muted/40"
                                        />
                                    </div>
                                </div>

                                <div className="flex flex-wrap justify-end gap-2 pt-2">
                                    <Button
                                        type="button"
                                        variant="outline"
                                        className="bg-white text-black"
                                        onClick={cancelar}
                                        disabled={publicando}
                                    >
                                        Cancelar
                                    </Button>
                                    <Button type="submit" disabled={formularioDeshabilitado}>
                                        {publicando
                                            ? "Publicando versión..."
                                            : "Publicar nueva versión"}
                                    </Button>
                                </div>
                            </CardContent>
                        </Card>
                    </form>
                )}
            </div>
        </AppShell>
    );
}

function CampoInfo({
    k,
    v,
    mono = false,
}: {
    k: string;
    v: string;
    mono?: boolean;
}) {
    return (
        <div>
            <dt className="text-xs uppercase tracking-wide text-muted-foreground">{k}</dt>
            <dd
                className={cn(
                    "mt-1 text-sm font-medium break-words",
                    mono ? "font-mono" : "",
                )}
            >
                {v}
            </dd>
        </div>
    );
}
