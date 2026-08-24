import { Link, createFileRoute, useNavigate } from "@tanstack/react-router";
import { AlertCircle, ArrowLeft, ChevronDown, Plus, Save, X } from "lucide-react";
import {
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
    type FormEvent,
    type ReactNode,
    type Ref,
} from "react";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { DropzoneArea } from "@/components/DropzoneArea";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import {
    Command,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
} from "@/components/ui/command";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { ApiError } from "@/lib/api";
import { listarAreas, type AreaCatalogo } from "@/lib/areas-api";
import {
    publicarDocumentoInicial,
    type DocumentoAlcance,
    type DocumentoPublicacionInicialRequestDto,
} from "@/lib/documentos-api";
import { aplicarMetadatosDesdeArchivo, resolverMetadatosCatalogoDesdeArchivo } from "@/lib/resolver-metadatos-publicacion";
import { obtenerIconoArea } from "@/lib/iconos-areas";
import { obtenerIconoFormato } from "@/lib/iconos-formatos";
import { obtenerIconoSubProceso } from "@/lib/iconos-subprocesos";
import { listarSubprogramas, type SubprogramaCatalogo } from "@/lib/subprogramas-api";
import { useIntranet } from "@/lib/store";
import { listarTiposDocumento, type TipoDocumentoCatalogo } from "@/lib/tipos-documento-api";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/app/publicar")({
    head: () => ({
        meta: [
            { title: "Publicar documento — Intranet documental" },
            {
                name: "description",
                content: "Registra y publica un nuevo documento en la intranet documental.",
            },
            { property: "og:title", content: "Publicar documento — Intranet documental" },
        ],
    }),
    component: NuevoDocumentoPage,
});

const LIMITE_ARCHIVO = 10 * 1024 * 1024;

const ORDEN_CAMPOS_VALIDACION = [
    "codigo",
    "titulo",
    "descripcion",
    "areaId",
    "subprogramaId",
    "tipoDocumentoId",
    "alcance",
    "areasAdicionalesIds",
    "archivo",
    "numeroVersionInicial",
    "descripcionVersionInicial",
] as const;

const SELECT_CONTENT_CLASS =
    "!bg-white !text-slate-900 border border-slate-200 shadow-2xl z-[99999]";

const SELECT_TRIGGER_CLASS = "w-full !bg-white !text-slate-900";

const SELECT_ITEM_CLASS =
    "!text-slate-900 focus:!bg-slate-100 focus:!text-slate-900 data-[highlighted]:!bg-slate-100 data-[highlighted]:!text-slate-900";

const POPOVER_CONTENT_CLASS =
    "w-[var(--radix-popover-trigger-width)] p-0 !bg-white !text-slate-900 border border-slate-200 shadow-2xl z-[99999]";

const COMMAND_CLASS = "!bg-white !text-slate-900";

const COMMAND_ITEM_CLASS =
    "!text-slate-900 data-[selected=true]:!bg-slate-100 data-[selected=true]:!text-slate-900";

const etiquetasAlcance: Record<DocumentoAlcance, string> = {
    AREA_RESPONSABLE: "Área responsable",
    AREAS_ESPECIFICAS: "Áreas específicas",
    GLOBAL: "Global",
};

const ayudaAlcance: Record<DocumentoAlcance, string> = {
    AREA_RESPONSABLE: "Visible para usuarios asociados al área responsable.",
    AREAS_ESPECIFICAS:
        "Visible para el área responsable y las áreas adicionales seleccionadas.",
    GLOBAL: "Visible para todos los usuarios autorizados del sistema.",
};

interface FormularioPublicacion {
    codigo: string;
    titulo: string;
    descripcion: string;
    areaId: string;
    subprogramaId: string;
    tipoDocumentoId: string;
    alcance: DocumentoAlcance | "";
    areasAdicionalesIds: string[];
    descripcionVersionInicial: string;
    numeroVersionInicial: string;
}

const formularioVacio: FormularioPublicacion = {
    codigo: "",
    titulo: "",
    descripcion: "",
    areaId: "",
    subprogramaId: "",
    tipoDocumentoId: "",
    alcance: "",
    areasAdicionalesIds: [],
    descripcionVersionInicial: "",
    numeroVersionInicial: "1",
};

function validarFormulario(
    form: FormularioPublicacion,
    archivo: File | null,
    areasActivas: AreaCatalogo[],
    subprogramas: SubprogramaCatalogo[],
    tiposActivos: TipoDocumentoCatalogo[],
): Record<string, string> {
    const errores: Record<string, string> = {};

    const codigo = form.codigo.trim();
    if (!codigo) errores.codigo = "El código del documento es obligatorio.";
    else if (codigo.length > 50) errores.codigo = "El código no puede superar los 50 caracteres.";

    const titulo = form.titulo.trim();
    if (!titulo) errores.titulo = "El nombre del documento es obligatorio.";
    else if (titulo.length > 200) errores.titulo = "El título no puede superar los 200 caracteres.";

    const descripcion = form.descripcion.trim();
    if (!descripcion) {
        errores.descripcion = "La descripción del documento es obligatoria.";
    } else if (descripcion.length > 500) {
        errores.descripcion = "La descripción no puede superar los 500 caracteres.";
    }

    if (!form.areaId) {
        errores.areaId = "Selecciona un área responsable.";
    } else if (!areasActivas.some((area) => area.id === form.areaId)) {
        errores.areaId = "Selecciona un área activa válida.";
    }

    if (!form.areaId) {
        errores.subprogramaId = "Selecciona primero un área responsable.";
    } else if (!form.subprogramaId) {
        errores.subprogramaId = "Selecciona un subproceso.";
    } else {
        const subprograma = subprogramas.find((item) => item.id === form.subprogramaId);
        if (!subprograma?.activo || subprograma.areaId !== form.areaId) {
            errores.subprogramaId = "Selecciona un subproceso activo del área elegida.";
        }
    }

    if (!form.tipoDocumentoId) {
        errores.tipoDocumentoId = "Selecciona un tipo de documento.";
    } else if (!tiposActivos.some((tipo) => tipo.id === form.tipoDocumentoId)) {
        errores.tipoDocumentoId = "Selecciona un tipo de documento activo válido.";
    }

    if (!form.alcance) {
        errores.alcance = "Selecciona el alcance del documento.";
    } else if (form.alcance === "AREAS_ESPECIFICAS") {
        if (form.areasAdicionalesIds.length === 0) {
            errores.areasAdicionalesIds = "Selecciona al menos un área autorizada.";
        } else if (form.areasAdicionalesIds.includes(form.areaId)) {
            errores.areasAdicionalesIds =
                "El área responsable no puede incluirse como área adicional.";
        }
    }

    if (!archivo) {
        errores.archivo = "Debes adjuntar un archivo para el documento.";
    } else {
        if (archivo.size > LIMITE_ARCHIVO) {
            errores.archivo = "El archivo no puede superar los 10 MB.";
        }
        if (archivo.name.toLowerCase().endsWith(".apk")) {
            errores.archivo = "No se permiten archivos APK.";
        }
    }

    const descripcionVersion = form.descripcionVersionInicial.trim();
    if (!descripcionVersion) {
        errores.descripcionVersionInicial = "La descripción de la versión inicial es obligatoria.";
    } else if (descripcionVersion.length > 500) {
        errores.descripcionVersionInicial =
            "La descripción de la versión no puede superar los 500 caracteres.";
    }

    const versionStr = form.numeroVersionInicial.trim();
    if (!versionStr) {
        errores.numeroVersionInicial = "La versión inicial es obligatoria.";
    } else {
        const versionNum = Number(versionStr);
        if (!Number.isInteger(versionNum) || versionNum < 1) {
            errores.numeroVersionInicial =
                "La versión inicial debe ser un entero mayor o igual a 1.";
        }
    }

    return errores;
}

function mostrarToastsValidacion(erroresValidacion: Record<string, string>) {
    for (const campo of ORDEN_CAMPOS_VALIDACION) {
        const mensaje = erroresValidacion[campo];
        if (mensaje) {
            toast.error(mensaje, { id: `publicar-${campo}` });
        }
    }
}

function construirMetadata(form: FormularioPublicacion): DocumentoPublicacionInicialRequestDto {
    const alcance = form.alcance as DocumentoAlcance;
    const descripcion = form.descripcion.trim();

    let areasAdicionalesIds: number[] = [];
    if (alcance === "AREAS_ESPECIFICAS") {
        areasAdicionalesIds = form.areasAdicionalesIds.map(Number);
    }

    return {
        codigo: form.codigo.trim(),
        titulo: form.titulo.trim(),
        descripcion: descripcion || null,
        areaId: Number(form.areaId),
        subprogramaId: Number(form.subprogramaId),
        tipoDocumentoId: Number(form.tipoDocumentoId),
        descripcionVersionInicial: form.descripcionVersionInicial.trim(),
        numeroVersionInicial: Number(form.numeroVersionInicial.trim()),
        alcance,
        areasAdicionalesIds,
    };
}

function NuevoDocumentoPage() {
    const navigate = useNavigate();
    const { permisos } = useIntranet();

    const [areasCatalogo, setAreasCatalogo] = useState<AreaCatalogo[]>([]);
    const [subprogramasCatalogo, setSubprogramasCatalogo] = useState<SubprogramaCatalogo[]>([]);
    const [tiposCatalogo, setTiposCatalogo] = useState<TipoDocumentoCatalogo[]>([]);
    const [cargandoCatalogos, setCargandoCatalogos] = useState(true);
    const [errorCatalogos, setErrorCatalogos] = useState<string | null>(null);
    const catalogosCargados = useRef(false);
    const pendienteMetadatosCatalogoRef = useRef<string | null>(null);

    const [form, setForm] = useState<FormularioPublicacion>(formularioVacio);
    const [archivo, setArchivo] = useState<File | null>(null);
    const [errores, setErrores] = useState<Record<string, string>>({});
    const [publicando, setPublicando] = useState(false);
    const [buscadorAreaAdicionalVisible, setBuscadorAreaAdicionalVisible] = useState(false);
    const refsCampos = useRef<Partial<Record<string, HTMLDivElement | null>>>({});

    const limpiarError = useCallback((campo: string) => {
        setErrores((prev) => {
            if (!(campo in prev)) return prev;
            const { [campo]: _, ...resto } = prev;
            return resto;
        });
    }, []);

    const registrarRefCampo = useCallback(
        (campo: string): Ref<HTMLDivElement> =>
            (elemento) => {
                refsCampos.current[campo] = elemento;
            },
        [],
    );

    const desplazarAlPrimerError = useCallback((erroresValidacion: Record<string, string>) => {
        for (const campo of ORDEN_CAMPOS_VALIDACION) {
            if (!erroresValidacion[campo]) continue;

            const contenedor = refsCampos.current[campo];
            if (!contenedor) continue;

            contenedor.scrollIntoView({ behavior: "smooth", block: "center" });

            const focusable = contenedor.querySelector<HTMLElement>(
                "input, textarea, button[role='combobox'], [tabindex='0']",
            );
            focusable?.focus({ preventScroll: true });
            break;
        }
    }, []);

    const areasActivas = useMemo(
        () => areasCatalogo.filter((area) => area.activo),
        [areasCatalogo],
    );

    const subprogramasActivos = useMemo(() => {
        if (!form.areaId) return [];
        return subprogramasCatalogo.filter(
            (item) => item.activo && item.areaId === form.areaId,
        );
    }, [subprogramasCatalogo, form.areaId]);

    const tiposActivos = useMemo(
        () => tiposCatalogo.filter((tipo) => tipo.activo),
        [tiposCatalogo],
    );

    const areasSeleccionablesAdicionales = useMemo(
        () =>
            areasActivas.filter(
                (area) =>
                    area.id !== form.areaId &&
                    !form.areasAdicionalesIds.includes(area.id),
            ),
        [areasActivas, form.areaId, form.areasAdicionalesIds],
    );

    const areasAdicionalesSeleccionadas = useMemo(
        () =>
            form.areasAdicionalesIds
                .map((id) => areasActivas.find((area) => area.id === id))
                .filter((area): area is AreaCatalogo => area != null),
        [form.areasAdicionalesIds, areasActivas],
    );

    const hayMasAreasAdicionales = areasSeleccionablesAdicionales.length > 0;

    const cargarCatalogos = useCallback(async (forzar = false) => {
        if (catalogosCargados.current && !forzar && !errorCatalogos) return;

        setCargandoCatalogos(true);
        setErrorCatalogos(null);
        try {
            const [areas, subprogramas, tipos] = await Promise.all([
                listarAreas(),
                listarSubprogramas(),
                listarTiposDocumento(),
            ]);
            setAreasCatalogo(areas);
            setSubprogramasCatalogo(subprogramas);
            setTiposCatalogo(tipos);
            catalogosCargados.current = true;
        } catch (err) {
            catalogosCargados.current = false;
            const mensaje =
                err instanceof ApiError
                    ? err.message
                    : "No fue posible cargar los catálogos del formulario.";
            setErrorCatalogos(mensaje);
        } finally {
            setCargandoCatalogos(false);
        }
    }, [errorCatalogos]);

    useEffect(() => {
        cargarCatalogos();
    }, [cargarCatalogos]);

    const cambiarArea = (areaId: string) => {
        setForm((prev) => ({
            ...prev,
            areaId,
            subprogramaId: "",
            areasAdicionalesIds: prev.areasAdicionalesIds.filter((id) => id !== areaId),
        }));
        limpiarError("areaId");
        limpiarError("subprogramaId");
    };

    const cambiarAlcance = (alcance: DocumentoAlcance) => {
        setForm((prev) => ({
            ...prev,
            alcance,
            areasAdicionalesIds: alcance === "AREAS_ESPECIFICAS" ? prev.areasAdicionalesIds : [],
        }));
        limpiarError("alcance");
        limpiarError("areasAdicionalesIds");
        if (alcance === "AREAS_ESPECIFICAS") {
            setBuscadorAreaAdicionalVisible(form.areasAdicionalesIds.length === 0);
        } else {
            setBuscadorAreaAdicionalVisible(false);
        }
    };

    const agregarAreaAdicional = (areaId: string) => {
        setForm((prev) => {
            if (
                prev.areasAdicionalesIds.includes(areaId) ||
                areaId === prev.areaId
            ) {
                return prev;
            }
            return {
                ...prev,
                areasAdicionalesIds: [...prev.areasAdicionalesIds, areaId],
            };
        });
        setBuscadorAreaAdicionalVisible(false);
        setErrores((prev) => {
            const { areasAdicionalesIds: _, ...resto } = prev;
            return resto;
        });
    };

    const quitarAreaAdicional = (areaId: string) => {
        setForm((prev) => {
            const areasAdicionalesIds = prev.areasAdicionalesIds.filter((id) => id !== areaId);
            if (areasAdicionalesIds.length === 0) {
                setBuscadorAreaAdicionalVisible(true);
            }
            return { ...prev, areasAdicionalesIds };
        });
    };

    const aplicarActualizacionesMetadatos = useCallback(
        (
            actualizaciones: Partial<FormularioPublicacion>,
            opciones?: { soloCamposVacios?: boolean },
        ) => {
            if (Object.keys(actualizaciones).length === 0) return;

            setForm((prev) => {
                const cambios: Partial<FormularioPublicacion> = {};
                for (const [campo, valor] of Object.entries(actualizaciones)) {
                    const clave = campo as keyof FormularioPublicacion;
                    if (opciones?.soloCamposVacios) {
                        const actual = prev[clave];
                        const vacio =
                            actual === "" ||
                            actual == null ||
                            (Array.isArray(actual) && actual.length === 0);
                        if (!vacio) continue;
                    }
                    (cambios as Record<string, unknown>)[clave] = valor;
                }
                return Object.keys(cambios).length > 0 ? { ...prev, ...cambios } : prev;
            });

            setErrores((prev) => {
                const resto = { ...prev };
                for (const campo of Object.keys(actualizaciones)) {
                    delete resto[campo];
                }
                return resto;
            });
        },
        [],
    );

    const procesarArchivo = useCallback(
        (file: File) => {
            if (file.size > LIMITE_ARCHIVO) {
                setArchivo(null);
                pendienteMetadatosCatalogoRef.current = null;
                setErrores((prev) => ({
                    ...prev,
                    archivo: "El archivo no puede superar los 10 MB.",
                }));
                return;
            }

            if (file.name.toLowerCase().endsWith(".apk")) {
                setArchivo(null);
                pendienteMetadatosCatalogoRef.current = null;
                setErrores((prev) => ({
                    ...prev,
                    archivo: "No se permiten archivos APK.",
                }));
                return;
            }

            setArchivo(file);
            limpiarError("archivo");

            const catalogosListos = catalogosCargados.current && !errorCatalogos;
            const resultadoMetadatos = aplicarMetadatosDesdeArchivo(
                file.name,
                subprogramasCatalogo,
                tiposCatalogo,
                { omitirResolucionCatalogo: !catalogosListos },
            );

            if (resultadoMetadatos.detectado) {
                aplicarActualizacionesMetadatos(resultadoMetadatos.actualizaciones);
                if (catalogosListos) {
                    pendienteMetadatosCatalogoRef.current = null;
                    toast.success("Metadatos detectados desde el nombre del archivo.");
                } else {
                    pendienteMetadatosCatalogoRef.current = file.name;
                }
            } else {
                pendienteMetadatosCatalogoRef.current = null;
            }

            if (catalogosListos) {
                for (const aviso of resultadoMetadatos.avisos) {
                    toast.info(aviso);
                }
            }
        },
        [
            aplicarActualizacionesMetadatos,
            errorCatalogos,
            limpiarError,
            subprogramasCatalogo,
            tiposCatalogo,
        ],
    );

    useEffect(() => {
        const nombrePendiente = pendienteMetadatosCatalogoRef.current;
        if (cargandoCatalogos || errorCatalogos || !archivo || !nombrePendiente) return;
        if (archivo.name !== nombrePendiente) return;

        const resultadoCatalogo = resolverMetadatosCatalogoDesdeArchivo(
            archivo.name,
            subprogramasCatalogo,
            tiposCatalogo,
        );

        pendienteMetadatosCatalogoRef.current = null;

        if (resultadoCatalogo.detectado) {
            aplicarActualizacionesMetadatos(resultadoCatalogo.actualizaciones, {
                soloCamposVacios: true,
            });
            toast.success("Metadatos detectados desde el nombre del archivo.");
        }

        for (const aviso of resultadoCatalogo.avisos) {
            toast.info(aviso);
        }
    }, [
        aplicarActualizacionesMetadatos,
        archivo,
        cargandoCatalogos,
        errorCatalogos,
        subprogramasCatalogo,
        tiposCatalogo,
    ]);

    const manejarRechazoArchivo = useCallback((mensaje: string) => {
        setArchivo(null);
        setErrores((prev) => ({ ...prev, archivo: mensaje }));
    }, []);

    const quitarArchivo = useCallback(() => {
        pendienteMetadatosCatalogoRef.current = null;
        setArchivo(null);
    }, []);

    const handleSubmit = async (evento: FormEvent<HTMLFormElement>) => {
        evento.preventDefault();

        if (cargandoCatalogos || errorCatalogos || publicando) return;

        const erroresValidacion = validarFormulario(
            form,
            archivo,
            areasActivas,
            subprogramasCatalogo,
            tiposActivos,
        );
        setErrores(erroresValidacion);

        if (Object.keys(erroresValidacion).length > 0) {
            mostrarToastsValidacion(erroresValidacion);
            desplazarAlPrimerError(erroresValidacion);
            return;
        }

        if (!archivo) return;

        const metadata = construirMetadata(form);

        setPublicando(true);
        try {
            const creado = await publicarDocumentoInicial(metadata, archivo);
            toast.success(`Documento ${creado.codigo} publicado correctamente.`);
            setForm(formularioVacio);
            setArchivo(null);
            setErrores({});
            navigate({ to: "/app/documentos" });
        } catch (err) {
            if (err instanceof ApiError) {
                if (err.status === 409) {
                    setErrores((prev) => ({ ...prev, codigo: err.message }));
                } else if (err.errores) {
                    const mapeados: Record<string, string> = {};
                    for (const [campo, mensaje] of Object.entries(err.errores)) {
                        mapeados[campo] = mensaje;
                    }
                    setErrores((prev) => ({ ...prev, ...mapeados }));
                }
                toast.error(err.message);
            } else {
                toast.error("No fue posible publicar el documento.");
            }
        } finally {
            setPublicando(false);
        }
    };

    if (!permisos?.publicarDocumentos) {
        return (
            <AppShell titulo="Publicar documento">
                <Card>
                    <CardContent className="py-14 text-center text-sm text-muted-foreground">
                        No cuentas con permisos para publicar nuevos documentos.
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    const formularioDeshabilitado = cargandoCatalogos || !!errorCatalogos || publicando;

    return (
        <AppShell
            titulo="Publicar nuevo documento"
            descripcion="Diligencia los metadatos del documento y adjunta el archivo oficial."
        >
            <div className="mx-auto max-w-4xl space-y-6 p-4 pb-10 md:p-6">
                <div className="mb-4 flex items-center space-x-4">
                    <Button variant="outline" size="icon" asChild>
                        <Link to="/app/gestion-documentos">
                            <ArrowLeft className="h-5 w-5" />
                        </Link>
                    </Button>
                    <div>
                        <h1 className="text-2xl font-bold tracking-tight">Publicar documento</h1>
                        <p className="text-sm text-muted-foreground">
                            Completa los metadatos y adjunta el archivo correspondiente.
                        </p>
                    </div>
                </div>

                {errorCatalogos ? (
                    <Card>
                        <CardContent className="space-y-3 py-8 text-center">
                            <p className="text-sm font-medium">{errorCatalogos}</p>
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={() => cargarCatalogos(true)}
                                disabled={cargandoCatalogos}
                            >
                                Reintentar
                            </Button>
                        </CardContent>
                    </Card>
                ) : cargandoCatalogos ? (
                    <Card>
                        <CardContent className="py-8 text-center text-sm text-muted-foreground">
                            Cargando catálogos del formulario...
                        </CardContent>
                    </Card>
                ) : null}

                <form onSubmit={handleSubmit} className="space-y-6">
                    <Card>
                        <CardHeader>
                            <CardTitle className="text-base font-bold">
                                Información del documento
                            </CardTitle>
                            <CardDescription>
                                Los metadatos permitirán clasificar y buscar el archivo en la
                                biblioteca.
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-5">
                            <Campo
                                id="codigo"
                                label="Código documental"
                                obligatorio
                                error={errores.codigo}
                                campoRef={registrarRefCampo("codigo")}
                            >
                                <Input
                                    id="codigo"
                                    placeholder="PR-L&D-PO-01"
                                    value={form.codigo}
                                    onChange={(e) => {
                                        setForm((prev) => ({ ...prev, codigo: e.target.value }));
                                        limpiarError("codigo");
                                    }}
                                    disabled={formularioDeshabilitado}
                                    aria-invalid={!!errores.codigo}
                                    aria-describedby={errores.codigo ? "codigo-error" : undefined}
                                    className={cn(errores.codigo && "border-destructive")}
                                />
                            </Campo>

                            <Campo
                                id="titulo"
                                label="Título"
                                obligatorio
                                error={errores.titulo}
                                campoRef={registrarRefCampo("titulo")}
                            >
                                <Input
                                    id="titulo"
                                    placeholder="Ej: Programa de limpieza y desinfección"
                                    value={form.titulo}
                                    onChange={(e) => {
                                        setForm((prev) => ({ ...prev, titulo: e.target.value }));
                                        limpiarError("titulo");
                                    }}
                                    disabled={formularioDeshabilitado}
                                    aria-invalid={!!errores.titulo}
                                    aria-describedby={errores.titulo ? "titulo-error" : undefined}
                                    className={cn(errores.titulo && "border-destructive")}
                                />
                            </Campo>

                            <Campo
                                id="descripcion"
                                label="Descripción"
                                obligatorio
                                error={errores.descripcion}
                                campoRef={registrarRefCampo("descripcion")}
                            >
                                <Textarea
                                    id="descripcion"
                                    rows={3}
                                    placeholder="Describe brevemente el contenido del documento…"
                                    value={form.descripcion}
                                    onChange={(e) => {
                                        setForm((prev) => ({
                                            ...prev,
                                            descripcion: e.target.value,
                                        }));
                                        limpiarError("descripcion");
                                    }}
                                    disabled={formularioDeshabilitado}
                                    aria-invalid={!!errores.descripcion}
                                    aria-describedby={
                                        errores.descripcion ? "descripcion-error" : undefined
                                    }
                                    className={cn(errores.descripcion && "border-destructive")}
                                />
                            </Campo>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader>
                            <CardTitle className="text-base font-bold">
                                Clasificación y alcance
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-5">
                            <div className="grid gap-4 sm:grid-cols-3">
                                <Campo
                                    id="areaId"
                                    label="Área responsable"
                                    obligatorio
                                    error={errores.areaId}
                                    campoRef={registrarRefCampo("areaId")}
                                >
                                    <Select
                                        value={form.areaId}
                                        onValueChange={cambiarArea}
                                        disabled={formularioDeshabilitado}
                                    >
                                        <SelectTrigger
                                            className={cn(
                                                SELECT_TRIGGER_CLASS,
                                                errores.areaId && "border-destructive",
                                            )}
                                            aria-invalid={!!errores.areaId}
                                            aria-describedby={
                                                errores.areaId ? "areaId-error" : undefined
                                            }
                                        >
                                            <SelectValue placeholder="Seleccionar…" />
                                        </SelectTrigger>
                                        <SelectContent className={SELECT_CONTENT_CLASS}>
                                            {areasActivas.map((area) => {
                                                const { icono: Icono, color } = obtenerIconoArea(
                                                    area.nombre,
                                                );
                                                return (
                                                    <SelectItem
                                                        className={SELECT_ITEM_CLASS}
                                                        key={area.id}
                                                        value={area.id}
                                                    >
                                                        <div className="flex items-center gap-2">
                                                            <Icono className={`size-4 ${color}`} />
                                                            <span>{area.nombre}</span>
                                                        </div>
                                                    </SelectItem>
                                                );
                                            })}
                                        </SelectContent>
                                    </Select>
                                </Campo>

                                <Campo
                                    id="subprogramaId"
                                    label="Subproceso"
                                    obligatorio
                                    error={errores.subprogramaId}
                                    campoRef={registrarRefCampo("subprogramaId")}
                                >
                                    <Select
                                        value={form.subprogramaId}
                                        onValueChange={(subprogramaId) => {
                                            setForm((prev) => ({ ...prev, subprogramaId }));
                                            limpiarError("subprogramaId");
                                        }}
                                        disabled={formularioDeshabilitado || !form.areaId}
                                    >
                                        <SelectTrigger
                                            className={cn(
                                                SELECT_TRIGGER_CLASS,
                                                errores.subprogramaId && "border-destructive",
                                            )}
                                            aria-invalid={!!errores.subprogramaId}
                                            aria-describedby={
                                                errores.subprogramaId
                                                    ? "subprogramaId-error"
                                                    : undefined
                                            }
                                        >
                                            <SelectValue
                                                placeholder={
                                                    form.areaId
                                                        ? "Seleccionar…"
                                                        : "Seleccione primero un área"
                                                }
                                            />
                                        </SelectTrigger>
                                        <SelectContent className={SELECT_CONTENT_CLASS}>
                                            {subprogramasActivos.map((subprograma) => {
                                                const { icono: Icono, color } =
                                                    obtenerIconoSubProceso(subprograma.nombre);
                                                return (
                                                    <SelectItem
                                                        className={SELECT_ITEM_CLASS}
                                                        key={subprograma.id}
                                                        value={subprograma.id}
                                                    >
                                                        <div className="flex items-center gap-2">
                                                            <Icono className={`size-4 ${color}`} />
                                                            <span>{subprograma.nombre}</span>
                                                        </div>
                                                    </SelectItem>
                                                );
                                            })}
                                        </SelectContent>
                                    </Select>
                                </Campo>

                                <Campo
                                    id="tipoDocumentoId"
                                    label="Tipo de documento"
                                    obligatorio
                                    error={errores.tipoDocumentoId}
                                    campoRef={registrarRefCampo("tipoDocumentoId")}
                                >
                                    <Select
                                        value={form.tipoDocumentoId}
                                        onValueChange={(tipoDocumentoId) => {
                                            setForm((prev) => ({ ...prev, tipoDocumentoId }));
                                            limpiarError("tipoDocumentoId");
                                        }}
                                        disabled={formularioDeshabilitado}
                                    >
                                        <SelectTrigger
                                            className={cn(
                                                SELECT_TRIGGER_CLASS,
                                                errores.tipoDocumentoId && "border-destructive",
                                            )}
                                            aria-invalid={!!errores.tipoDocumentoId}
                                            aria-describedby={
                                                errores.tipoDocumentoId
                                                    ? "tipoDocumentoId-error"
                                                    : undefined
                                            }
                                        >
                                            <SelectValue placeholder="Seleccionar…" />
                                        </SelectTrigger>
                                        <SelectContent className={SELECT_CONTENT_CLASS}>
                                            {tiposActivos.map((tipo) => {
                                                const { icono: Icono, color } = obtenerIconoFormato(
                                                    tipo.nombre,
                                                );
                                                return (
                                                    <SelectItem
                                                        className={SELECT_ITEM_CLASS}
                                                        key={tipo.id}
                                                        value={tipo.id}
                                                    >
                                                        <div className="flex items-center gap-2">
                                                            <Icono className={`size-4 ${color}`} />
                                                            <span>{tipo.nombre}</span>
                                                        </div>
                                                    </SelectItem>
                                                );
                                            })}
                                        </SelectContent>
                                    </Select>
                                </Campo>
                            </div>

                            <Campo
                                id="alcance"
                                label="Alcance"
                                obligatorio
                                error={errores.alcance}
                                campoRef={registrarRefCampo("alcance")}
                            >
                                <Select
                                    value={form.alcance}
                                    onValueChange={(valor) =>
                                        cambiarAlcance(valor as DocumentoAlcance)
                                    }
                                    disabled={formularioDeshabilitado}
                                >
                                    <SelectTrigger
                                        className={cn(
                                            SELECT_TRIGGER_CLASS,
                                            errores.alcance && "border-destructive",
                                        )}
                                        aria-invalid={!!errores.alcance}
                                        aria-describedby={
                                            errores.alcance ? "alcance-error" : undefined
                                        }
                                    >
                                        <SelectValue placeholder="Seleccionar alcance…" />
                                    </SelectTrigger>
                                    <SelectContent className={SELECT_CONTENT_CLASS}>
                                        {(Object.keys(etiquetasAlcance) as DocumentoAlcance[]).map(
                                            (alcance) => (
                                                <SelectItem
                                                    className={SELECT_ITEM_CLASS}
                                                    key={alcance}
                                                    value={alcance}
                                                >
                                                    {etiquetasAlcance[alcance]}
                                                </SelectItem>
                                            ),
                                        )}
                                    </SelectContent>
                                </Select>
                                {form.alcance ? (
                                    <p className="text-xs text-muted-foreground">
                                        {ayudaAlcance[form.alcance]}
                                    </p>
                                ) : null}
                            </Campo>

                            {form.alcance === "AREAS_ESPECIFICAS" ? (
                                <div
                                    ref={registrarRefCampo("areasAdicionalesIds")}
                                    className={cn(
                                        "space-y-3 rounded-md border p-4",
                                        errores.areasAdicionalesIds && "border-destructive",
                                    )}
                                >
                                    <div>
                                        <Label>
                                            Áreas adicionales
                                            <span className="text-destructive"> *</span>
                                        </Label>
                                        <p className="text-xs text-muted-foreground">
                                            Seleccione al menos un área distinta del área
                                            responsable.
                                        </p>
                                    </div>

                                    {buscadorAreaAdicionalVisible && hayMasAreasAdicionales ? (
                                        <BuscadorAreaAdicional
                                            areas={areasSeleccionablesAdicionales}
                                            onSeleccionar={agregarAreaAdicional}
                                            disabled={formularioDeshabilitado}
                                        />
                                    ) : null}

                                    {areasAdicionalesSeleccionadas.length > 0 ? (
                                        <div className="space-y-2">
                                            <p className="text-xs font-medium text-muted-foreground">
                                                Áreas seleccionadas:
                                            </p>
                                            <div className="flex flex-wrap gap-2">
                                                {areasAdicionalesSeleccionadas.map((area) => {
                                                    const { icono: Icono, color } =
                                                        obtenerIconoArea(area.nombre);
                                                    return (
                                                        <div
                                                            key={area.id}
                                                            className="flex items-center gap-2 rounded-md border border-emerald-500/30 bg-emerald-500/5 px-3 py-1.5"
                                                        >
                                                            <Icono
                                                                className={`size-4 ${color}`}
                                                            />
                                                            <span className="text-sm font-medium text-slate-900">
                                                                {area.nombre}
                                                            </span>
                                                            <button
                                                                type="button"
                                                                onClick={() =>
                                                                    quitarAreaAdicional(area.id)
                                                                }
                                                                disabled={formularioDeshabilitado}
                                                                className="rounded-sm p-0.5 text-muted-foreground hover:bg-muted hover:text-foreground disabled:opacity-50"
                                                                aria-label={`Quitar ${area.nombre}`}
                                                            >
                                                                <X className="size-3.5" />
                                                            </button>
                                                        </div>
                                                    );
                                                })}
                                            </div>
                                        </div>
                                    ) : null}

                                    {hayMasAreasAdicionales ? (
                                        <Button
                                            type="button"
                                            variant="outline"
                                            size="sm"
                                            className="gap-1.5 !bg-white !text-slate-900"
                                            onClick={() => setBuscadorAreaAdicionalVisible(true)}
                                            disabled={
                                                formularioDeshabilitado ||
                                                buscadorAreaAdicionalVisible
                                            }
                                        >
                                            <Plus className="size-4" />
                                            Agregar otra área
                                        </Button>
                                    ) : areasAdicionalesSeleccionadas.length > 0 ? (
                                        <p className="text-xs text-muted-foreground">
                                            No hay más áreas disponibles.
                                        </p>
                                    ) : null}

                                    {errores.areasAdicionalesIds ? (
                                        <p
                                            id="areasAdicionalesIds-error"
                                            role="alert"
                                            className="flex items-start gap-1.5 text-sm text-destructive"
                                        >
                                            <AlertCircle
                                                className="mt-0.5 size-3.5 shrink-0"
                                                aria-hidden
                                            />
                                            <span>{errores.areasAdicionalesIds}</span>
                                        </p>
                                    ) : null}
                                </div>
                            ) : null}
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader>
                            <CardTitle className="text-base font-bold">Archivo y versión</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-5">
                            <Campo
                                id="archivo"
                                label="Archivo"
                                obligatorio
                                error={errores.archivo}
                                campoRef={registrarRefCampo("archivo")}
                            >
                                <DropzoneArea
                                    archivo={archivo}
                                    disabled={publicando || !!errorCatalogos}
                                    error={errores.archivo}
                                    maxSizeBytes={LIMITE_ARCHIVO}
                                    onArchivoSeleccionado={procesarArchivo}
                                    onQuitarArchivo={quitarArchivo}
                                    onRechazo={manejarRechazoArchivo}
                                />
                            </Campo>

                            <Campo
                                id="numeroVersionInicial"
                                label="Versión inicial"
                                obligatorio
                                error={errores.numeroVersionInicial}
                                campoRef={registrarRefCampo("numeroVersionInicial")}
                            >
                                <Input
                                    id="numeroVersionInicial"
                                    type="number"
                                    min={1}
                                    step={1}
                                    value={form.numeroVersionInicial}
                                    onChange={(e) => {
                                        setForm((prev) => ({
                                            ...prev,
                                            numeroVersionInicial: e.target.value,
                                        }));
                                        limpiarError("numeroVersionInicial");
                                    }}
                                    disabled={formularioDeshabilitado}
                                    aria-invalid={!!errores.numeroVersionInicial}
                                    aria-describedby={
                                        errores.numeroVersionInicial
                                            ? "numeroVersionInicial-error"
                                            : undefined
                                    }
                                    className={cn(
                                        errores.numeroVersionInicial && "border-destructive",
                                    )}
                                />
                                <p className="text-xs text-muted-foreground">
                                    Indica la versión actual del documento.
                                </p>
                            </Campo>

                            <Campo
                                id="descripcionVersionInicial"
                                label="Descripción de la versión inicial"
                                obligatorio
                                error={errores.descripcionVersionInicial}
                                campoRef={registrarRefCampo("descripcionVersionInicial")}
                            >
                                <Textarea
                                    id="descripcionVersionInicial"
                                    rows={3}
                                    placeholder="Indique brevemente qué contiene esta primera versión."
                                    value={form.descripcionVersionInicial}
                                    onChange={(e) => {
                                        setForm((prev) => ({
                                            ...prev,
                                            descripcionVersionInicial: e.target.value,
                                        }));
                                        limpiarError("descripcionVersionInicial");
                                    }}
                                    disabled={formularioDeshabilitado}
                                    aria-invalid={!!errores.descripcionVersionInicial}
                                    aria-describedby={
                                        errores.descripcionVersionInicial
                                            ? "descripcionVersionInicial-error"
                                            : undefined
                                    }
                                    className={cn(
                                        errores.descripcionVersionInicial && "border-destructive",
                                    )}
                                />
                            </Campo>

                            <div className="flex justify-end gap-3 border-t pt-4">
                                <Button
                                    asChild
                                    variant="outline"
                                    type="button"
                                    disabled={publicando}
                                >
                                    <Link to="/app/gestion-documentos">Cancelar</Link>
                                </Button>
                                <Button
                                    type="submit"
                                    disabled={publicando || formularioDeshabilitado}
                                    className="gap-2"
                                >
                                    <Save className="size-4" />
                                    {publicando ? "Publicando..." : "Publicar documento"}
                                </Button>
                            </div>
                        </CardContent>
                    </Card>
                </form>
            </div>
        </AppShell>
    );
}

function Campo({
    id,
    label,
    obligatorio = false,
    error,
    children,
    campoRef,
}: {
    id?: string;
    label: string;
    obligatorio?: boolean;
    error?: string;
    children: ReactNode;
    campoRef?: Ref<HTMLDivElement>;
}) {
    const errorId = id ? `${id}-error` : undefined;

    return (
        <div ref={campoRef} className="space-y-1.5">
            <Label htmlFor={id}>
                {label}
                {obligatorio ? <span className="text-destructive"> *</span> : null}
            </Label>
            {children}
            {error ? (
                <p
                    id={errorId}
                    role="alert"
                    className="flex items-start gap-1.5 text-sm text-destructive"
                >
                    <AlertCircle className="mt-0.5 size-3.5 shrink-0" aria-hidden />
                    <span>{error}</span>
                </p>
            ) : null}
        </div>
    );
}

function BuscadorAreaAdicional({
    areas,
    onSeleccionar,
    disabled = false,
}: {
    areas: AreaCatalogo[];
    onSeleccionar: (areaId: string) => void;
    disabled?: boolean;
}) {
    const [abierto, setAbierto] = useState(false);

    return (
        <Popover open={abierto} onOpenChange={setAbierto}>
            <PopoverTrigger asChild>
                <Button
                    type="button"
                    variant="outline"
                    role="combobox"
                    aria-expanded={abierto}
                    disabled={disabled || areas.length === 0}
                    className={cn(
                        "w-full justify-between font-normal !bg-white !text-slate-900",
                        !areas.length && "text-muted-foreground",
                    )}
                >
                    Buscar y seleccionar área…
                    <ChevronDown className="ml-2 size-4 shrink-0 opacity-50" />
                </Button>
            </PopoverTrigger>
            <PopoverContent align="start" className={POPOVER_CONTENT_CLASS}>
                <Command className={COMMAND_CLASS}>
                    <CommandInput placeholder="Buscar área…" className="!text-slate-900" />
                    <CommandList>
                        <CommandEmpty>No se encontraron áreas.</CommandEmpty>
                        <CommandGroup>
                            {areas.map((area) => {
                                const { icono: Icono, color } = obtenerIconoArea(area.nombre);
                                return (
                                    <CommandItem
                                        key={area.id}
                                        value={area.nombre}
                                        className={COMMAND_ITEM_CLASS}
                                        onSelect={() => {
                                            onSeleccionar(area.id);
                                            setAbierto(false);
                                        }}
                                    >
                                        <Icono className={`size-4 ${color}`} />
                                        <span>{area.nombre}</span>
                                    </CommandItem>
                                );
                            })}
                        </CommandGroup>
                    </CommandList>
                </Command>
            </PopoverContent>
        </Popover>
    );
}
