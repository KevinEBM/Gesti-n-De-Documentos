import { Link, createFileRoute, useNavigate } from "@tanstack/react-router";
import { Search, Upload, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { toast } from "sonner";

import {
    DocumentoAcciones,
    DocumentoEstadoAdminSelect,
    DocumentoEstadoBadge,
    DocumentoFiltroSelect,
} from "@/components/documentos-consulta-ui";
import { AppShell } from "@/components/AppShell";
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ApiError } from "@/lib/api";
import { listarAreas, type AreaCatalogo } from "@/lib/areas-api";
import {
    ALERT_DIALOG_ACTION_CLASS,
    ALERT_DIALOG_CANCEL_CLASS,
    ALERT_DIALOG_CONTENT_CLASS,
    ALERT_DIALOG_DESCRIPTION_CLASS,
    ALERT_DIALOG_TITLE_CLASS,
    ALERT_DIALOG_TITLE_DESACTIVAR_CLASS,
    construirFiltrosApi,
    dispararDescargaEnNavegador,
    etiquetasAlcance,
    filtrosVacios,
    formatFechaDocumento,
    hayFiltrosActivos,
    TODOS,
    type FiltrosDocumentos,
} from "@/lib/documentos-consulta-shared";
import {
    actualizarEstadoDocumento,
    descargarVersionVigente,
    listarDocumentos,
    type DocumentoEstado,
    type DocumentoResumen,
} from "@/lib/documentos-api";
import { obtenerIconoFormato } from "@/lib/iconos-formatos";
import { obtenerIconoSubProceso } from "@/lib/iconos-subprocesos";
import { listarSubprogramas, type SubprogramaCatalogo } from "@/lib/subprogramas-api";
import { useIntranet } from "@/lib/store";
import { listarTiposDocumento, type TipoDocumentoCatalogo } from "@/lib/tipos-documento-api";

export const Route = createFileRoute("/app/gestion-documentos")({
    head: () => ({
        meta: [
            { title: "Gestión de documentos — Intranet documental" },
            {
                name: "description",
                content:
                    "Consulta y administra documentos internos: filtra por área, subprograma, tipo, estado y fechas.",
            },
            {
                property: "og:title",
                content: "Gestión de documentos — Intranet documental",
            },
            {
                property: "og:description",
                content:
                    "Consulta documentos internos con filtros avanzados y descarga de versiones vigentes.",
            },
        ],
    }),
    component: GestionDocumentos,
});

const OPCIONES_ESTADO = [
    { v: "PUBLICADO", l: "Publicado" },
    { v: "INACTIVO", l: "Inactivo" },
    { v: "OBSOLETO", l: "Obsoleto" },
];

type ConfirmacionEstadoPendiente = {
    documento: DocumentoResumen;
    destino: DocumentoEstado;
};

function mensajeConfirmacionEstado(destino: DocumentoEstado): { titulo: string; descripcion: string } {
    if (destino === "INACTIVO") {
        return {
            titulo: "Desactivar publicación",
            descripcion:
                "Esta publicación dejará de ser visible para los demás usuarios. Podrás activarla nuevamente.",
        };
    }
    if (destino === "OBSOLETO") {
        return {
            titulo: "Obsoleto",
            descripcion:
                "Esta publicación dejará de ser visible y no podrá recibir nuevas versiones mientras esté obsoleta. Podrás activarla nuevamente.",
        };
    }
    return { titulo: "", descripcion: "" };
}

function mensajeExitoEstado(destino: DocumentoEstado): string {
    switch (destino) {
        case "PUBLICADO":
            return "Publicación activada correctamente.";
        case "INACTIVO":
            return "Publicación desactivada correctamente.";
        case "OBSOLETO":
            return "Publicación marcada como obsoleta.";
    }
}

function GestionDocumentos() {
    const navigate = useNavigate();
    const { permisos } = useIntranet();

    const [areasCatalogo, setAreasCatalogo] = useState<AreaCatalogo[]>([]);
    const [subprogramasCatalogo, setSubprogramasCatalogo] = useState<SubprogramaCatalogo[]>([]);
    const [tiposCatalogo, setTiposCatalogo] = useState<TipoDocumentoCatalogo[]>([]);
    const [cargandoCatalogos, setCargandoCatalogos] = useState(true);
    const [errorCatalogos, setErrorCatalogos] = useState<string | null>(null);
    const catalogosCargados = useRef(false);

    const [filtrosFormulario, setFiltrosFormulario] = useState(filtrosVacios);
    const [filtrosAplicados, setFiltrosAplicados] = useState(filtrosVacios);
    const [errorFechas, setErrorFechas] = useState<string | null>(null);

    const [documentos, setDocumentos] = useState<DocumentoResumen[]>([]);
    const [pagina, setPagina] = useState(0);
    const [totalPaginas, setTotalPaginas] = useState(0);
    const [totalElementos, setTotalElementos] = useState(0);
    const [cargando, setCargando] = useState(true);
    const [errorCarga, setErrorCarga] = useState<string | null>(null);
    const requestIdRef = useRef(0);

    const [descargandoId, setDescargandoId] = useState<string | null>(null);
    const [alternandoEstadoId, setAlternandoEstadoId] = useState<string | null>(null);
    const [confirmacionEstado, setConfirmacionEstado] = useState<ConfirmacionEstadoPendiente | null>(
        null,
    );

    const areasActivas = useMemo(
        () => areasCatalogo.filter((area) => area.activo),
        [areasCatalogo],
    );

    const subprogramasActivos = useMemo(() => {
        if (filtrosFormulario.area === TODOS) {
            return [];
        }
        return subprogramasCatalogo.filter(
            (item) => item.activo && item.areaId === filtrosFormulario.area,
        );
    }, [subprogramasCatalogo, filtrosFormulario.area]);

    const tiposActivos = useMemo(
        () => tiposCatalogo.filter((tipo) => tipo.activo),
        [tiposCatalogo],
    );

    const requiereSeleccionArea = filtrosFormulario.area === TODOS;

    const filtrosAplicadosActivos = useMemo(
        () => hayFiltrosActivos(filtrosAplicados),
        [filtrosAplicados],
    );

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
                    : "No fue posible cargar los catálogos de filtros.";
            setErrorCatalogos(mensaje);
        } finally {
            setCargandoCatalogos(false);
        }
    }, [errorCatalogos]);

    const cargarDocumentos = useCallback(async (filtros: FiltrosDocumentos, page: number) => {
        const requestId = ++requestIdRef.current;
        setCargando(true);
        setErrorCarga(null);

        try {
            const resultado = await listarDocumentos(construirFiltrosApi(filtros, page));
            if (requestId !== requestIdRef.current) return;

            setDocumentos(resultado.contenido);
            setPagina(resultado.pagina);
            setTotalPaginas(resultado.totalPaginas);
            setTotalElementos(resultado.totalElementos);
        } catch (err) {
            if (requestId !== requestIdRef.current) return;
            const mensaje =
                err instanceof ApiError
                    ? err.message
                    : "No fue posible cargar los documentos.";
            setErrorCarga(mensaje);
        } finally {
            if (requestId === requestIdRef.current) {
                setCargando(false);
            }
        }
    }, []);

    useEffect(() => {
        let activo = true;

        const inicializar = async () => {
            await cargarCatalogos();
            if (!activo) return;
            await cargarDocumentos(filtrosVacios, 0);
        };

        inicializar();
        return () => {
            activo = false;
            requestIdRef.current += 1;
        };
        // Carga inicial única al montar.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const cambiarArea = (area: string) => {
        setFiltrosFormulario((prev) => ({
            ...prev,
            area,
            subprograma: TODOS,
        }));
    };

    const aplicarFiltros = () => {
        if (
            filtrosFormulario.fechaDesde &&
            filtrosFormulario.fechaHasta &&
            filtrosFormulario.fechaDesde > filtrosFormulario.fechaHasta
        ) {
            setErrorFechas("La fecha inicial no puede ser posterior a la fecha final.");
            return;
        }

        setErrorFechas(null);
        setFiltrosAplicados(filtrosFormulario);
        cargarDocumentos(filtrosFormulario, 0);
    };

    const limpiarFiltros = () => {
        setFiltrosFormulario(filtrosVacios);
        setFiltrosAplicados(filtrosVacios);
        setErrorFechas(null);
        cargarDocumentos(filtrosVacios, 0);
    };

    const irPaginaAnterior = () => {
        if (pagina <= 0 || cargando) return;
        cargarDocumentos(filtrosAplicados, pagina - 1);
    };

    const irPaginaSiguiente = () => {
        if (cargando || pagina >= totalPaginas - 1) return;
        cargarDocumentos(filtrosAplicados, pagina + 1);
    };

    const descargarDocumento = useCallback(async (documento: DocumentoResumen) => {
        setDescargandoId(documento.id);
        try {
            const { blob, nombreArchivo } = await descargarVersionVigente(documento.id);
            const nombre = nombreArchivo ?? (documento.codigo || "documento");
            dispararDescargaEnNavegador(blob, nombre);
        } catch (err) {
            const mensaje =
                err instanceof ApiError
                    ? err.message
                    : "No fue posible descargar el documento.";
            toast.error(mensaje);
        } finally {
            setDescargandoId(null);
        }
    }, []);

    const verDocumento = useCallback(
        (documento: DocumentoResumen) => {
            navigate({ to: "/app/documento/$id", params: { id: documento.id } });
        },
        [navigate],
    );

    const editarDocumento = useCallback(
        (documento: DocumentoResumen) => {
            navigate({ to: "/app/documento/$id/editar", params: { id: documento.id } });
        },
        [navigate],
    );

    const aplicarCambioEstado = useCallback(
        async (documento: DocumentoResumen, destino: DocumentoEstado) => {
            setAlternandoEstadoId(documento.id);
            try {
                const actualizado = await actualizarEstadoDocumento(documento.id, destino);
                setDocumentos((prev) =>
                    prev.map((item) =>
                        item.id === documento.id
                            ? { ...item, estado: actualizado.estado }
                            : item,
                    ),
                );
                toast.success(mensajeExitoEstado(destino));
            } catch (err) {
                const mensaje =
                    err instanceof ApiError
                        ? err.message
                        : "No fue posible cambiar el estado del documento.";
                toast.error(mensaje);
            } finally {
                setAlternandoEstadoId(null);
            }
        },
        [],
    );

    const solicitarCambioEstado = useCallback(
        (documento: DocumentoResumen, destino: DocumentoEstado, requiereConfirmacion: boolean) => {
            if (requiereConfirmacion) {
                setConfirmacionEstado({ documento, destino });
                return;
            }
            void aplicarCambioEstado(documento, destino);
        },
        [aplicarCambioEstado],
    );

    const confirmarCambioEstado = useCallback(async () => {
        if (!confirmacionEstado) return;
        const { documento, destino } = confirmacionEstado;
        setConfirmacionEstado(null);
        await aplicarCambioEstado(documento, destino);
    }, [aplicarCambioEstado, confirmacionEstado]);

    if (!permisos.actualizarDocumentos) {
        return (
            <AppShell titulo="Gestión de documentos">
                <Card>
                    <CardContent className="py-14 text-center text-sm text-muted-foreground">
                        No cuentas con permisos para administrar documentos.
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    const contenidoListado = () => {
        if (cargando) {
            return (
                <Card>
                    <CardContent className="py-14 text-center text-sm text-muted-foreground">
                        Cargando documentos...
                    </CardContent>
                </Card>
            );
        }

        if (errorCarga) {
            return (
                <Card>
                    <CardContent className="space-y-3 py-14 text-center">
                        <p className="text-sm font-medium">{errorCarga}</p>
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={() => cargarDocumentos(filtrosAplicados, pagina)}
                        >
                            Reintentar
                        </Button>
                    </CardContent>
                </Card>
            );
        }

        if (totalElementos === 0) {
            return (
                <Card>
                    <CardContent className="py-14 text-center">
                        <p className="text-sm font-medium">
                            {filtrosAplicadosActivos
                                ? "No se encontraron documentos con los filtros seleccionados."
                                : "No hay documentos disponibles."}
                        </p>
                    </CardContent>
                </Card>
            );
        }

        return (
            <Card className="overflow-hidden py-0">
                <Table>
                    <TableHeader>
                        <TableRow className="bg-secondary/60">
                            <TableHead>Código</TableHead>
                            <TableHead>Título</TableHead>
                            <TableHead>Subprograma</TableHead>
                            <TableHead>Tipo</TableHead>
                            <TableHead>Alcance</TableHead>
                            <TableHead>Estado</TableHead>
                            <TableHead>Actualización</TableHead>
                            <TableHead className="text-right">Acciones</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {documentos.map((documento) => {
                            const { icono: IconoTipo, color: colorTipo } = obtenerIconoFormato(
                                documento.tipoDocumentoNombre,
                            );
                            const { icono: IconoSub, color: colorSub } = obtenerIconoSubProceso(
                                documento.subprogramaNombre,
                            );

                            return (
                                <TableRow key={documento.id}>
                                    <TableCell className="font-mono font-semibold whitespace-nowrap">
                                        {documento.codigo}
                                    </TableCell>
                                    <TableCell className="max-w-xs truncate font-medium">
                                        {documento.titulo}
                                    </TableCell>
                                    <TableCell className="text-sm whitespace-nowrap">
                                        <div className="flex items-center gap-1.5">
                                            <IconoSub className={`size-4 ${colorSub}`} />
                                            <span>{documento.subprogramaNombre}</span>
                                        </div>
                                    </TableCell>
                                    <TableCell className="text-sm whitespace-nowrap">
                                        <div className="flex items-center gap-1.5">
                                            <IconoTipo className={`size-4 ${colorTipo}`} />
                                            <span>{documento.tipoDocumentoNombre}</span>
                                        </div>
                                    </TableCell>
                                    <TableCell className="text-sm whitespace-nowrap">
                                        {etiquetasAlcance[documento.alcance]}
                                    </TableCell>
                                    <TableCell>
                                        {permisos.administrarEstados ? (
                                            <DocumentoEstadoAdminSelect
                                                estado={documento.estado}
                                                deshabilitado={alternandoEstadoId === documento.id}
                                                onSolicitarCambio={(destino, requiereConfirmacion) =>
                                                    solicitarCambioEstado(
                                                        documento,
                                                        destino,
                                                        requiereConfirmacion,
                                                    )
                                                }
                                            />
                                        ) : (
                                            <DocumentoEstadoBadge estado={documento.estado} />
                                        )}
                                    </TableCell>
                                    <TableCell className="text-sm whitespace-nowrap">
                                        {formatFechaDocumento(documento.fechaActualizacion)}
                                    </TableCell>
                                    <TableCell className="text-right">
                                        <DocumentoAcciones
                                            documento={documento}
                                            descargandoId={descargandoId}
                                            onDescargar={descargarDocumento}
                                            onVer={verDocumento}
                                            onEditar={editarDocumento}
                                        />
                                    </TableCell>
                                </TableRow>
                            );
                        })}
                    </TableBody>
                </Table>
            </Card>
        );
    };

    return (
        <AppShell
            titulo="Gestión de documentos"
            descripcion={`${totalElementos} documento(s) encontrados`}
            acciones={
                <Button asChild size="sm" className="gap-2">
                    <Link to="/app/publicar">
                        <Upload className="size-4" />
                        Publicar documento
                    </Link>
                </Button>
            }
        >
            <Card>
                <CardContent className="space-y-4 py-5">
                    <div className="grid gap-3 sm:grid-cols-2">
                        <div className="relative">
                            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                            <Input
                                placeholder="Código del documento…"
                                className="pl-9"
                                value={filtrosFormulario.codigo}
                                onChange={(e) =>
                                    setFiltrosFormulario((prev) => ({
                                        ...prev,
                                        codigo: e.target.value,
                                    }))
                                }
                            />
                        </div>
                        <Input
                            placeholder="Título del documento…"
                            value={filtrosFormulario.titulo}
                            onChange={(e) =>
                                setFiltrosFormulario((prev) => ({
                                    ...prev,
                                    titulo: e.target.value,
                                }))
                            }
                        />
                    </div>

                    {errorCatalogos ? (
                        <div className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm">
                            <span>{errorCatalogos}</span>
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={() => cargarCatalogos(true)}
                                disabled={cargandoCatalogos}
                            >
                                Reintentar catálogos
                            </Button>
                        </div>
                    ) : cargandoCatalogos ? (
                        <p className="text-sm text-muted-foreground">Cargando catálogos de filtros...</p>
                    ) : null}

                    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
                        <DocumentoFiltroSelect
                            label="Área"
                            value={filtrosFormulario.area}
                            onChange={cambiarArea}
                            opciones={areasActivas.map((area) => ({ v: area.id, l: area.nombre }))}
                            tipoFiltro="area"
                            disabled={cargandoCatalogos || !!errorCatalogos}
                        />
                        <DocumentoFiltroSelect
                            label="Subprograma"
                            value={filtrosFormulario.subprograma}
                            onChange={(subprograma) =>
                                setFiltrosFormulario((prev) => ({ ...prev, subprograma }))
                            }
                            opciones={subprogramasActivos.map((item) => ({
                                v: item.id,
                                l: item.nombre,
                            }))}
                            tipoFiltro="subproceso"
                            disabled={cargandoCatalogos || !!errorCatalogos || requiereSeleccionArea}
                            placeholder={
                                requiereSeleccionArea ? "Seleccione primero un área" : undefined
                            }
                        />
                        <DocumentoFiltroSelect
                            label="Tipo"
                            value={filtrosFormulario.tipo}
                            onChange={(tipo) =>
                                setFiltrosFormulario((prev) => ({ ...prev, tipo }))
                            }
                            opciones={tiposActivos.map((tipo) => ({ v: tipo.id, l: tipo.nombre }))}
                            tipoFiltro="tipo"
                            disabled={cargandoCatalogos || !!errorCatalogos}
                        />
                        <DocumentoFiltroSelect
                            label="Estado"
                            value={filtrosFormulario.estado}
                            onChange={(estado) =>
                                setFiltrosFormulario((prev) => ({ ...prev, estado }))
                            }
                            opciones={OPCIONES_ESTADO}
                        />
                        <div className="space-y-1.5">
                            <Label className="text-xs text-muted-foreground">Desde</Label>
                            <Input
                                type="date"
                                value={filtrosFormulario.fechaDesde}
                                onChange={(e) =>
                                    setFiltrosFormulario((prev) => ({
                                        ...prev,
                                        fechaDesde: e.target.value,
                                    }))
                                }
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label className="text-xs text-muted-foreground">Hasta</Label>
                            <Input
                                type="date"
                                value={filtrosFormulario.fechaHasta}
                                onChange={(e) =>
                                    setFiltrosFormulario((prev) => ({
                                        ...prev,
                                        fechaHasta: e.target.value,
                                    }))
                                }
                            />
                        </div>
                    </div>

                    {errorFechas ? (
                        <p className="text-sm text-destructive">{errorFechas}</p>
                    ) : null}

                    <div className="flex justify-end gap-2">
                        <Button variant="ghost" size="sm" onClick={limpiarFiltros} className="gap-1.5">
                            <X className="size-4" /> Limpiar
                        </Button>
                        <Button size="sm" onClick={aplicarFiltros} className="gap-1.5">
                            <Search className="size-4" /> Buscar
                        </Button>
                    </div>
                </CardContent>
            </Card>

            {contenidoListado()}

            {!cargando && !errorCarga && totalElementos > 0 ? (
                <div className="flex flex-wrap items-center justify-between gap-3">
                    <p className="text-sm text-muted-foreground">
                        Página {pagina + 1} de {Math.max(totalPaginas, 1)}
                    </p>
                    <div className="flex gap-2">
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={irPaginaAnterior}
                            disabled={pagina <= 0}
                        >
                            Anterior
                        </Button>
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={irPaginaSiguiente}
                            disabled={pagina >= totalPaginas - 1}
                        >
                            Siguiente
                        </Button>
                    </div>
                </div>
            ) : null}

            <AlertDialog
                open={confirmacionEstado !== null}
                onOpenChange={(abierto) => {
                    if (!abierto) setConfirmacionEstado(null);
                }}
            >
                <AlertDialogContent className={ALERT_DIALOG_CONTENT_CLASS}>
                    <AlertDialogHeader>
                        <AlertDialogTitle
                            className={
                                confirmacionEstado?.destino === "INACTIVO"
                                    ? ALERT_DIALOG_TITLE_DESACTIVAR_CLASS
                                    : ALERT_DIALOG_TITLE_CLASS
                            }
                        >
                            {confirmacionEstado
                                ? mensajeConfirmacionEstado(confirmacionEstado.destino).titulo
                                : ""}
                        </AlertDialogTitle>
                        <AlertDialogDescription className={ALERT_DIALOG_DESCRIPTION_CLASS}>
                            {confirmacionEstado
                                ? mensajeConfirmacionEstado(confirmacionEstado.destino).descripcion
                                : ""}
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel className={ALERT_DIALOG_CANCEL_CLASS}>
                            Cancelar
                        </AlertDialogCancel>
                        <AlertDialogAction
                            className={ALERT_DIALOG_ACTION_CLASS}
                            onClick={() => void confirmarCambioEstado()}
                        >
                            Confirmar
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </AppShell>
    );
}
