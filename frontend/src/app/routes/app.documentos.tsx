import { createFileRoute } from "@tanstack/react-router";
import { Download, Eye, LayoutGrid, List, Search, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState, type ComponentType } from "react";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ApiError, apiFetch } from "@/lib/api";
import { listarAreas, type AreaCatalogo } from "@/lib/areas-api";
import {
    descargarVersionVigente,
    listarDocumentos,
    type DocumentoAlcance,
    type DocumentoEstado,
    type DocumentoFiltros,
    type DocumentoResumen,
} from "@/lib/documentos-api";
import { obtenerIconoArea } from "@/lib/iconos-areas";
import { obtenerIconoFormato } from "@/lib/iconos-formatos";
import { obtenerIconoSubProceso } from "@/lib/iconos-subprocesos";
import { listarSubprogramas, type SubprogramaCatalogo } from "@/lib/subprogramas-api";
import { useIntranet } from "@/lib/store";
import { listarTiposDocumento, mapTipoDocumentoResponseDto, type TipoDocumentoCatalogo, type TipoDocumentoResponseDto } from "@/lib/tipos-documento-api";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/app/documentos")({
    head: () => ({
        meta: [
            { title: "Biblioteca de documentos — Intranet documental" },
            {
                name: "description",
                content: "Busca y filtra manuales, políticas, protocolos y procedimientos por área, sub-proceso, tipo y fecha.",
            },
            { property: "og:title", content: "Biblioteca de documentos — Intranet documental" },
            { property: "og:description", content: "Busca y filtra documentos internos por área, sub-proceso, tipo y fecha." },
        ],
    }),
    component: Biblioteca,
});

const TODOS = "todos";
const TAMANO_PAGINA = 20;

const SELECT_CONTENT_CLASS =
    "!bg-white !text-slate-900 border border-slate-200 shadow-2xl z-[99999]";

const SELECT_TRIGGER_CLASS = "w-full !bg-white !text-slate-900";

const SELECT_ITEM_CLASS =
    "!text-slate-900 focus:!bg-slate-100 focus:!text-slate-900 data-[highlighted]:!bg-slate-100 data-[highlighted]:!text-slate-900";

async function listarTiposConsultaBiblioteca(): Promise<TipoDocumentoCatalogo[]> {
    const datos = await apiFetch<TipoDocumentoResponseDto[]>("/api/tipos-documento/activos");
    return datos.map(mapTipoDocumentoResponseDto);
}

interface FiltrosDocumentos {
    codigo: string;
    titulo: string;
    area: string;
    subprograma: string;
    tipo: string;
    estado: string;
    fechaDesde: string;
    fechaHasta: string;
}

const filtrosVacios: FiltrosDocumentos = {
    codigo: "",
    titulo: "",
    area: TODOS,
    subprograma: TODOS,
    tipo: TODOS,
    estado: TODOS,
    fechaDesde: "",
    fechaHasta: "",
};

const etiquetasAlcance: Record<DocumentoAlcance, string> = {
    AREA_RESPONSABLE: "Área responsable",
    AREAS_ESPECIFICAS: "Áreas específicas",
    GLOBAL: "Global",
};

const estilosEstado: Record<DocumentoEstado, string> = {
    PUBLICADO: "border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400",
    INACTIVO: "border-zinc-500/30 bg-zinc-500/10 text-zinc-600 dark:text-zinc-400",
    OBSOLETO: "border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400",
};

const etiquetasEstado: Record<DocumentoEstado, string> = {
    PUBLICADO: "Publicado",
    INACTIVO: "Inactivo",
    OBSOLETO: "Obsoleto",
};

function formatFecha(fecha: string): string {
    try {
        return new Intl.DateTimeFormat("es-CO", {
            dateStyle: "medium",
            timeStyle: "short",
        }).format(new Date(fecha));
    } catch {
        return fecha;
    }
}

function hayFiltrosActivos(filtros: FiltrosDocumentos): boolean {
    return (
        !!filtros.codigo.trim() ||
        !!filtros.titulo.trim() ||
        filtros.area !== TODOS ||
        filtros.subprograma !== TODOS ||
        filtros.tipo !== TODOS ||
        filtros.estado !== TODOS ||
        !!filtros.fechaDesde ||
        !!filtros.fechaHasta
    );
}

function construirFiltrosBase(esAdmin: boolean, areasActivas: AreaCatalogo[]): FiltrosDocumentos {
    if (!esAdmin && areasActivas.length === 1) {
        return { ...filtrosVacios, area: areasActivas[0].id };
    }
    return filtrosVacios;
}

function construirFiltrosApi(filtros: FiltrosDocumentos, page: number): DocumentoFiltros {
    const api: DocumentoFiltros = { page, size: TAMANO_PAGINA };

    const codigo = filtros.codigo.trim();
    const titulo = filtros.titulo.trim();

    if (codigo) api.codigo = codigo;
    if (titulo) api.titulo = titulo;
    if (filtros.area !== TODOS) api.areaId = Number(filtros.area);
    if (filtros.subprograma !== TODOS) api.subprogramaId = Number(filtros.subprograma);
    if (filtros.tipo !== TODOS) api.tipoDocumentoId = Number(filtros.tipo);
    if (filtros.estado !== TODOS) api.estado = filtros.estado as DocumentoEstado;
    if (filtros.fechaDesde) api.fechaDesde = filtros.fechaDesde;
    if (filtros.fechaHasta) api.fechaHasta = filtros.fechaHasta;

    return api;
}

function DocumentoEstadoBadge({ estado }: { estado: DocumentoEstado }) {
    return (
        <Badge variant="outline" className={cn("gap-1.5 font-medium", estilosEstado[estado])}>
            <span className="size-1.5 rounded-full bg-current" />
            {etiquetasEstado[estado]}
        </Badge>
    );
}

function dispararDescargaEnNavegador(blob: Blob, nombreArchivo: string) {
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement("a");
    enlace.href = url;
    enlace.download = nombreArchivo;
    enlace.click();
    URL.revokeObjectURL(url);
}

function Biblioteca() {
    const { sesion } = useIntranet();
    const esAdmin = sesion?.rol === "administrador";

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

    const [vista, setVista] = useState<"tabla" | "tarjetas">("tabla");
    const [descargandoId, setDescargandoId] = useState<string | null>(null);

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

    const tiposActivos = useMemo(() => tiposCatalogo, [tiposCatalogo]);

    const areaUnicaPreseleccionada = !esAdmin && areasActivas.length === 1;
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
                esAdmin ? listarTiposDocumento() : listarTiposConsultaBiblioteca(),
            ]);
            setAreasCatalogo(areas);
            setSubprogramasCatalogo(subprogramas);
            setTiposCatalogo(tipos);
            catalogosCargados.current = true;

            const areasActivasCargadas = areas.filter((area) => area.activo);
            if (!esAdmin && areasActivasCargadas.length === 1) {
                const areaId = areasActivasCargadas[0].id;
                setFiltrosFormulario((prev) =>
                    prev.area === TODOS ? { ...prev, area: areaId, subprograma: TODOS } : prev,
                );
                setFiltrosAplicados((prev) =>
                    prev.area === TODOS ? { ...prev, area: areaId, subprograma: TODOS } : prev,
                );
            }
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
    }, [errorCatalogos, esAdmin]);

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
        const base = construirFiltrosBase(esAdmin, areasActivas);
        setFiltrosFormulario(base);
        setFiltrosAplicados(base);
        setErrorFechas(null);
        cargarDocumentos(base, 0);
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

    const opcionesEstado = esAdmin
        ? [
              { v: "PUBLICADO", l: "Publicado" },
              { v: "INACTIVO", l: "Inactivo" },
              { v: "OBSOLETO", l: "Obsoleto" },
          ]
        : [{ v: "PUBLICADO", l: "Publicado" }];

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

        if (vista === "tabla") {
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
                                            <DocumentoEstadoBadge estado={documento.estado} />
                                        </TableCell>
                                        <TableCell className="text-sm whitespace-nowrap">
                                            {formatFecha(documento.fechaActualizacion)}
                                        </TableCell>
                                        <TableCell className="text-right">
                                            <AccionesDocumento
                                                documento={documento}
                                                descargandoId={descargandoId}
                                                onDescargar={descargarDocumento}
                                            />
                                        </TableCell>
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                </Card>
            );
        }

        return (
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                {documentos.map((documento) => {
                    const { icono: IconoTipo, color: colorTipo } = obtenerIconoFormato(
                        documento.tipoDocumentoNombre,
                    );
                    const { icono: IconoSub, color: colorSub } = obtenerIconoSubProceso(
                        documento.subprogramaNombre,
                    );

                    return (
                        <Card key={documento.id}>
                            <CardContent className="space-y-3 py-5">
                                <div className="flex items-start justify-between gap-2">
                                    <div className="min-w-0">
                                        <p className="font-mono text-xs text-muted-foreground">
                                            {documento.codigo}
                                        </p>
                                        <p className="font-medium leading-snug">{documento.titulo}</p>
                                    </div>
                                    <DocumentoEstadoBadge estado={documento.estado} />
                                </div>
                                <dl className="grid grid-cols-2 gap-2 text-xs">
                                    <Dato
                                        k="Tipo"
                                        v={documento.tipoDocumentoNombre}
                                        icon={IconoTipo}
                                        color={colorTipo}
                                    />
                                    <Dato
                                        k="Subprograma"
                                        v={documento.subprogramaNombre}
                                        icon={IconoSub}
                                        color={colorSub}
                                    />
                                    <Dato k="Alcance" v={etiquetasAlcance[documento.alcance]} />
                                    <Dato
                                        k="Actualización"
                                        v={formatFecha(documento.fechaActualizacion)}
                                    />
                                </dl>
                                <AccionesDocumento
                                    documento={documento}
                                    descargandoId={descargandoId}
                                    onDescargar={descargarDocumento}
                                    apilado
                                />
                            </CardContent>
                        </Card>
                    );
                })}
            </div>
        );
    };

    return (
        <AppShell
            titulo="Biblioteca de documentos"
            descripcion={`${totalElementos} documento(s) encontrados`}
            acciones={
                <div className="hidden items-center gap-1 rounded-md border border-border p-0.5 md:flex">
                    <Button
                        variant={vista === "tabla" ? "secondary" : "ghost"}
                        size="sm"
                        onClick={() => setVista("tabla")}
                        className="gap-1.5"
                    >
                        <List className="size-4" /> Lista
                    </Button>
                    <Button
                        variant={vista === "tarjetas" ? "secondary" : "ghost"}
                        size="sm"
                        onClick={() => setVista("tarjetas")}
                        className="gap-1.5"
                    >
                        <LayoutGrid className="size-4" /> Tarjetas
                    </Button>
                </div>
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
                        <Filtro
                            label="Área"
                            value={filtrosFormulario.area}
                            onChange={cambiarArea}
                            opciones={areasActivas.map((area) => ({ v: area.id, l: area.nombre }))}
                            tipoFiltro="area"
                            disabled={
                                cargandoCatalogos ||
                                !!errorCatalogos ||
                                areaUnicaPreseleccionada
                            }
                        />
                        <Filtro
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
                                requiereSeleccionArea
                                    ? "Seleccione primero un área"
                                    : undefined
                            }
                        />
                        <Filtro
                            label="Tipo"
                            value={filtrosFormulario.tipo}
                            onChange={(tipo) =>
                                setFiltrosFormulario((prev) => ({ ...prev, tipo }))
                            }
                            opciones={tiposActivos.map((tipo) => ({ v: tipo.id, l: tipo.nombre }))}
                            tipoFiltro="tipo"
                            disabled={cargandoCatalogos || !!errorCatalogos}
                        />
                        <Filtro
                            label="Estado"
                            value={filtrosFormulario.estado}
                            onChange={(estado) =>
                                setFiltrosFormulario((prev) => ({ ...prev, estado }))
                            }
                            opciones={opcionesEstado}
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
                            <X className="size-4" /> Limpiar filtros
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
        </AppShell>
    );
}

function Dato({
    k,
    v,
    icon: Icon,
    color,
}: {
    k: string;
    v: string;
    icon?: ComponentType<{ className?: string }>;
    color?: string;
}) {
    return (
        <div>
            <dt className="text-muted-foreground">{k}</dt>
            <dd className="flex items-center gap-1.5 font-medium">
                {Icon ? <Icon className={`size-3.5 ${color}`} /> : null}
                <span>{v}</span>
            </dd>
        </div>
    );
}

function AccionesDocumento({
    documento,
    descargandoId,
    onDescargar,
    apilado = false,
}: {
    documento: DocumentoResumen;
    descargandoId: string | null;
    onDescargar: (documento: DocumentoResumen) => void;
    apilado?: boolean;
}) {
    const descargando = descargandoId === documento.id;

    return (
        <div
            className={cn(
                "flex gap-2",
                apilado ? "flex-col sm:flex-row" : "flex-wrap items-center justify-end",
            )}
        >
            <Button
                size="sm"
                variant="outline"
                disabled
                title="El detalle estará disponible próximamente."
                className="gap-1.5"
            >
                <Eye className="size-4" />
                Ver
            </Button>
            <Button
                size="sm"
                variant="outline"
                className="gap-1.5"
                disabled={descargando}
                onClick={() => onDescargar(documento)}
            >
                <Download className="size-4" />
                {descargando ? "Descargando..." : "Descargar"}
            </Button>
        </div>
    );
}

function Filtro({
    label,
    value,
    onChange,
    opciones,
    tipoFiltro,
    disabled = false,
    placeholder,
}: {
    label: string;
    value: string;
    onChange: (v: string) => void;
    opciones: { v: string; l: string }[];
    tipoFiltro?: "area" | "subproceso" | "tipo";
    disabled?: boolean;
    placeholder?: string;
}) {
    return (
        <div className="space-y-1.5">
            <Label className="text-xs text-muted-foreground">{label}</Label>
            <Select value={value} onValueChange={onChange} disabled={disabled}>
                <SelectTrigger className={SELECT_TRIGGER_CLASS}>
                    <SelectValue placeholder={placeholder} />
                </SelectTrigger>
                <SelectContent className={SELECT_CONTENT_CLASS}>
                    <SelectItem value={TODOS} className={SELECT_ITEM_CLASS}>
                        Todos
                    </SelectItem>
                    {opciones.map((opcion) => {
                        let Icono: ComponentType<{ className?: string }> | undefined;
                        let color: string | undefined;

                        if (tipoFiltro === "area") {
                            const res = obtenerIconoArea(opcion.l);
                            Icono = res.icono;
                            color = res.color;
                        } else if (tipoFiltro === "subproceso") {
                            const res = obtenerIconoSubProceso(opcion.l);
                            Icono = res.icono;
                            color = res.color;
                        } else if (tipoFiltro === "tipo") {
                            const res = obtenerIconoFormato(opcion.l);
                            Icono = res.icono;
                            color = res.color;
                        }

                        return (
                            <SelectItem key={opcion.v} value={opcion.v} className={SELECT_ITEM_CLASS}>
                                <div className="flex items-center gap-2">
                                    {Icono ? <Icono className={`size-4 ${color}`} /> : null}
                                    <span>{opcion.l}</span>
                                </div>
                            </SelectItem>
                        );
                    })}
                </SelectContent>
            </Select>
        </div>
    );
}
