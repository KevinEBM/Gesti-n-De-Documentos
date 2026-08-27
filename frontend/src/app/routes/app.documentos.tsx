import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { LayoutGrid, List, Search, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState, type ComponentType } from "react";
import { toast } from "sonner";

import {
    DocumentoAcciones,
    DocumentoEstadoBadge,
    DocumentoFiltroSelect,
} from "@/components/documentos-consulta-ui";
import { AppShell } from "@/components/AppShell";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ApiError } from "@/lib/api";
import { listarAreas, type AreaCatalogo } from "@/lib/areas-api";
import {
    construirFiltrosApi,
    construirFiltrosBaseConsulta,
    dispararDescargaEnNavegador,
    etiquetaCatalogoConsulta,
    etiquetasAlcance,
    areaConsultaNoAdminBloqueada,
    filtrarSubprogramasConsulta,
    filtrosVacios,
    formatFechaDocumento,
    hayFiltrosActivos,
    resolverAreaIdEfectivaConsulta,
    resolverAreaObligatoriaNoAdmin,
    subprocesoConsultaDeshabilitado,
    TODOS,
    type FiltrosDocumentos,
} from "@/lib/documentos-consulta-shared";
import {
    descargarVersionVigente,
    listarDocumentos,
    type DocumentoResumen,
} from "@/lib/documentos-api";
import { obtenerIconoFormato } from "@/lib/iconos-formatos";
import { obtenerIconoSubProceso } from "@/lib/iconos-subprocesos";
import { listarSubprogramas, type SubprogramaCatalogo } from "@/lib/subprogramas-api";
import { useIntranet } from "@/lib/store";
import { listarTiposDocumentoConsulta, type TipoDocumentoCatalogo } from "@/lib/tipos-documento-api";

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

function opcionesFiltrosApi(esAdmin: boolean) {
    return { incluirAreaEnConsulta: esAdmin };
}

function Biblioteca() {
    const navigate = useNavigate();
    const { sesion, sincronizarAreaDesdeCatalogo } = useIntranet();
    const esAdmin = sesion?.rol === "administrador";

    const [areasCatalogo, setAreasCatalogo] = useState<AreaCatalogo[]>([]);
    const [areasApiCargadas, setAreasApiCargadas] = useState(false);
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

    const areaObligatoriaNoAdmin = resolverAreaObligatoriaNoAdmin(
        esAdmin,
        areasCatalogo,
        areasApiCargadas,
        sesion?.areaId,
        sesion?.areaPrincipalNombre,
    );
    const areaFiltroBloqueada = areaConsultaNoAdminBloqueada(esAdmin);
    const areaIdEfectiva = resolverAreaIdEfectivaConsulta({
        esAdmin,
        filtroArea: filtrosFormulario.area,
        areasUsuario: areasCatalogo,
        areasApiCargadas,
        sesionAreaId: sesion?.areaId,
        sesionAreaNombre: sesion?.areaPrincipalNombre,
    });

    const subprogramasFiltro = useMemo(
        () =>
            filtrarSubprogramasConsulta(subprogramasCatalogo, {
                esAdmin,
                areaIdEfectiva,
            }),
        [subprogramasCatalogo, esAdmin, areaIdEfectiva],
    );

    const opcionesAreasFiltro = useMemo(
        () =>
            areasCatalogo.map((area) => ({
                v: area.id,
                l: etiquetaCatalogoConsulta(area.nombre, area.activo, "femenino"),
            })),
        [areasCatalogo],
    );

    const opcionesSubprogramasFiltro = useMemo(
        () =>
            subprogramasFiltro.map((item) => ({
                v: item.id,
                l: etiquetaCatalogoConsulta(item.nombre ?? "", item.activo ?? true, "masculino"),
            })),
        [subprogramasFiltro],
    );

    const opcionesTiposFiltro = useMemo(
        () =>
            tiposCatalogo.map((tipo) => ({
                v: tipo.id,
                l: etiquetaCatalogoConsulta(tipo.nombre, tipo.activo, "masculino"),
            })),
        [tiposCatalogo],
    );

    const filtrosAplicadosActivos = useMemo(
        () =>
            hayFiltrosActivos(filtrosAplicados, {
                areaNoCuentaComoFiltro: areaObligatoriaNoAdmin,
            }),
        [filtrosAplicados, areaObligatoriaNoAdmin],
    );

    const cargarCatalogos = useCallback(async (forzar = false): Promise<AreaCatalogo[]> => {
        if (catalogosCargados.current && !forzar && !errorCatalogos) {
            return areasCatalogo;
        }

        setCargandoCatalogos(true);
        setErrorCatalogos(null);
        try {
            const [areas, subprogramas, tipos] = await Promise.all([
                listarAreas(),
                listarSubprogramas(),
                listarTiposDocumentoConsulta(esAdmin),
            ]);
            setAreasCatalogo(areas);
            setAreasApiCargadas(true);
            setSubprogramasCatalogo(subprogramas);
            setTiposCatalogo(tipos);
            catalogosCargados.current = true;
            sincronizarAreaDesdeCatalogo(areas, true);

            if (!esAdmin) {
                const areaId = resolverAreaObligatoriaNoAdmin(
                    esAdmin,
                    areas,
                    true,
                    sesion?.areaId,
                    sesion?.areaPrincipalNombre,
                );
                if (areaId) {
                    setFiltrosFormulario((prev) =>
                        prev.area === TODOS || prev.area !== areaId
                            ? { ...prev, area: areaId, subprograma: TODOS }
                            : prev,
                    );
                    setFiltrosAplicados((prev) =>
                        prev.area === TODOS || prev.area !== areaId
                            ? { ...prev, area: areaId, subprograma: TODOS }
                            : prev,
                    );
                } else {
                    setFiltrosFormulario((prev) =>
                        prev.area !== TODOS ? { ...prev, area: TODOS, subprograma: TODOS } : prev,
                    );
                    setFiltrosAplicados((prev) =>
                        prev.area !== TODOS ? { ...prev, area: TODOS, subprograma: TODOS } : prev,
                    );
                }
            }

            return areas;
        } catch (err) {
            catalogosCargados.current = false;
            setAreasApiCargadas(false);
            const mensaje =
                err instanceof ApiError
                    ? err.message
                    : "No fue posible cargar los catálogos de filtros.";
            setErrorCatalogos(mensaje);
            return [];
        } finally {
            setCargandoCatalogos(false);
        }
    }, [areasCatalogo, errorCatalogos, esAdmin, sincronizarAreaDesdeCatalogo, sesion?.areaId, sesion?.areaPrincipalNombre]);

    const cargarDocumentos = useCallback(
        async (filtros: FiltrosDocumentos, page: number) => {
            const requestId = ++requestIdRef.current;
            setCargando(true);
            setErrorCarga(null);

            try {
                const resultado = await listarDocumentos(
                    construirFiltrosApi(filtros, page, opcionesFiltrosApi(esAdmin)),
                );
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
        },
        [esAdmin],
    );

    useEffect(() => {
        let activo = true;

        const inicializar = async () => {
            const areas = await cargarCatalogos();
            if (!activo) return;

            const filtrosIniciales = construirFiltrosBaseConsulta(
                esAdmin,
                areas,
                true,
                sesion?.areaId,
                sesion?.areaPrincipalNombre,
            );
            setFiltrosFormulario(filtrosIniciales);
            setFiltrosAplicados(filtrosIniciales);
            await cargarDocumentos(filtrosIniciales, 0);
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
        const base = construirFiltrosBaseConsulta(
            esAdmin,
            areasCatalogo,
            areasApiCargadas,
            sesion?.areaId,
            sesion?.areaPrincipalNombre,
        );
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

    const verDocumento = useCallback(
        (documento: DocumentoResumen) => {
            navigate({ to: "/app/documento/$id", params: { id: documento.id } });
        },
        [navigate],
    );

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
                                <TableHead>Subproceso</TableHead>
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
                                            {formatFechaDocumento(documento.fechaActualizacion)}
                                        </TableCell>
                                        <TableCell className="text-right">
                                            <DocumentoAcciones
                                                documento={documento}
                                                descargandoId={descargandoId}
                                                onDescargar={descargarDocumento}
                                                onVer={verDocumento}
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
                                        k="Subproceso"
                                        v={documento.subprogramaNombre}
                                        icon={IconoSub}
                                        color={colorSub}
                                    />
                                    <Dato k="Alcance" v={etiquetasAlcance[documento.alcance]} />
                                    <Dato
                                        k="Actualización"
                                        v={formatFechaDocumento(documento.fechaActualizacion)}
                                    />
                                </dl>
                                <DocumentoAcciones
                                    documento={documento}
                                    descargandoId={descargandoId}
                                    onDescargar={descargarDocumento}
                                    onVer={verDocumento}
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
                <div className="flex items-center gap-1 rounded-md border border-border p-0.5">
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

                    <div className="grid min-w-0 grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-[minmax(0,1.4fr)_minmax(0,1.4fr)_minmax(0,1.15fr)_minmax(8.5rem,0.65fr)_minmax(10rem,0.85fr)_minmax(10rem,0.85fr)]">
                        <DocumentoFiltroSelect
                            label="Área"
                            value={filtrosFormulario.area}
                            onChange={cambiarArea}
                            opciones={opcionesAreasFiltro}
                            tipoFiltro="area"
                            ocultarTodos={areaFiltroBloqueada}
                            disabled={
                                cargandoCatalogos ||
                                !!errorCatalogos ||
                                areaFiltroBloqueada
                            }
                        />
                        <DocumentoFiltroSelect
                            label="Subproceso"
                            value={filtrosFormulario.subprograma}
                            onChange={(subprograma) =>
                                setFiltrosFormulario((prev) => ({ ...prev, subprograma }))
                            }
                            opciones={opcionesSubprogramasFiltro}
                            tipoFiltro="subproceso"
                            disabled={
                                cargandoCatalogos ||
                                !!errorCatalogos ||
                                subprocesoConsultaDeshabilitado(
                                    esAdmin,
                                    areaIdEfectiva,
                                    cargandoCatalogos,
                                    errorCatalogos,
                                )
                            }
                            placeholder={
                                subprocesoConsultaDeshabilitado(
                                    esAdmin,
                                    areaIdEfectiva,
                                    false,
                                    null,
                                )
                                    ? "Seleccione primero un área"
                                    : undefined
                            }
                        />
                        <DocumentoFiltroSelect
                            label="Tipo"
                            value={filtrosFormulario.tipo}
                            onChange={(tipo) =>
                                setFiltrosFormulario((prev) => ({ ...prev, tipo }))
                            }
                            opciones={opcionesTiposFiltro}
                            tipoFiltro="tipo"
                            disabled={cargandoCatalogos || !!errorCatalogos}
                        />
                        <DocumentoFiltroSelect
                            label="Estado"
                            value={filtrosFormulario.estado}
                            onChange={(estado) =>
                                setFiltrosFormulario((prev) => ({ ...prev, estado }))
                            }
                            opciones={opcionesEstado}
                        />
                        <div className="min-w-0 space-y-1.5">
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
                        <div className="min-w-0 space-y-1.5">
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

                    <div className="flex flex-wrap items-center justify-between gap-3">
                        <label className="flex cursor-pointer items-center gap-2 text-sm text-muted-foreground">
                            <Checkbox
                                checked={filtrosFormulario.soloGlobales}
                                onCheckedChange={(checked) =>
                                    setFiltrosFormulario((prev) => ({
                                        ...prev,
                                        soloGlobales: checked === true,
                                    }))
                                }
                                disabled={cargandoCatalogos || !!errorCatalogos}
                            />
                            Solo globales
                        </label>
                        <div className="flex gap-2">
                            <Button variant="ghost" size="sm" onClick={limpiarFiltros} className="gap-1.5">
                                <X className="size-4" /> Limpiar filtros
                            </Button>
                            <Button size="sm" onClick={aplicarFiltros} className="gap-1.5">
                                <Search className="size-4" /> Buscar
                            </Button>
                        </div>
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
