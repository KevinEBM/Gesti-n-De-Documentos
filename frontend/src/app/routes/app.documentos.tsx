import { Link, createFileRoute } from "@tanstack/react-router";
import { Eye, LayoutGrid, List, Search, X } from "lucide-react";
import { useMemo, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { EstadoBadge } from "@/components/EstadoBadge";
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
import { useIntranet } from "@/lib/store";

export const Route = createFileRoute("/app/documentos")({
    head: () => ({
        meta: [
            { title: "Biblioteca de documentos — Intranet documental" },
            {
                name: "description",
                content: "Busca y filtra manuales, políticas, protocolos y procedimientos por área, categoría, tipo y fecha.",
            },
            { property: "og:title", content: "Biblioteca de documentos — Intranet documental" },
            { property: "og:description", content: "Busca y filtra documentos internos por área, categoría, tipo y fecha." },
        ],
    }),
    component: Biblioteca,
});

const TODOS = "todos";

function Biblioteca() {
    const { documentosVisibles, areas, categorias, tipos, nombreArea, nombreCategoria, nombreTipo, sesion } =
        useIntranet();
    const esAdmin = sesion?.rol === "administrador";

    const [busqueda, setBusqueda] = useState("");
    const [area, setArea] = useState(TODOS);
    const [categoria, setCategoria] = useState(TODOS);
    const [tipo, setTipo] = useState(TODOS);
    const [estado, setEstado] = useState(TODOS);
    const [desde, setDesde] = useState("");
    const [hasta, setHasta] = useState("");
    const [vista, setVista] = useState<"tabla" | "tarjetas">("tabla");

    const visibles = documentosVisibles;


    const resultados = useMemo(
        () =>
            visibles
                .filter((d) => {
                    const q = busqueda.trim().toLowerCase();
                    if (q && !`${d.nombre} ${d.descripcion}`.toLowerCase().includes(q)) return false;
                    if (area !== TODOS && d.areaId !== area) return false;
                    if (categoria !== TODOS && d.categoriaId !== categoria) return false;
                    if (tipo !== TODOS && d.tipoId !== tipo) return false;
                    if (estado !== TODOS && d.estado !== estado) return false;
                    if (desde && d.fechaPublicacion < desde) return false;
                    if (hasta && d.fechaPublicacion > hasta) return false;
                    return true;
                })
                .sort((a, b) => b.fechaPublicacion.localeCompare(a.fechaPublicacion)),
        [visibles, busqueda, area, categoria, tipo, estado, desde, hasta],
    );

    const limpiar = () => {
        setBusqueda("");
        setArea(TODOS);
        setCategoria(TODOS);
        setTipo(TODOS);
        setEstado(TODOS);
        setDesde("");
        setHasta("");
    };

    return (
        <AppShell
            titulo="Biblioteca de documentos"
            descripcion={`${resultados.length} documento(s) encontrados`}
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
                    <div className="relative">
                        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                        <Input
                            placeholder="Buscar por nombre o descripción del documento…"
                            className="pl-9"
                            value={busqueda}
                            onChange={(e) => setBusqueda(e.target.value)}
                        />
                    </div>

                    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
                        <Filtro label="Área" value={area} onChange={setArea} opciones={areas.map((a) => ({ v: a.id, l: a.nombre }))} />
                        <Filtro
                            label="Categoría"
                            value={categoria}
                            onChange={setCategoria}
                            opciones={categorias.map((c) => ({ v: c.id, l: c.nombre }))}
                        />
                        <Filtro label="Tipo" value={tipo} onChange={setTipo} opciones={tipos.map((t) => ({ v: t.id, l: t.nombre }))} />
                        <Filtro
                            label="Estado"
                            value={estado}
                            onChange={setEstado}
                            opciones={
                                esAdmin
                                    ? [
                                        { v: "publicado", l: "Publicado" },
                                        { v: "borrador", l: "Borrador" },
                                        { v: "inactivo", l: "Inactivo" },
                                    ]
                                    : [{ v: "publicado", l: "Publicado" }]
                            }
                        />
                        <div className="space-y-1.5">
                            <Label className="text-xs text-muted-foreground">Desde</Label>
                            <Input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} />
                        </div>
                        <div className="space-y-1.5">
                            <Label className="text-xs text-muted-foreground">Hasta</Label>
                            <Input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} />
                        </div>
                    </div>

                    <div className="flex justify-end">
                        <Button variant="ghost" size="sm" onClick={limpiar} className="gap-1.5">
                            <X className="size-4" /> Limpiar filtros
                        </Button>
                    </div>
                </CardContent>
            </Card>

            {resultados.length === 0 ? (
                <Card>
                    <CardContent className="py-14 text-center">
                        <p className="text-sm font-medium">No se encontraron documentos</p>
                        <p className="mt-1 text-sm text-muted-foreground">Ajusta la búsqueda o los filtros aplicados.</p>
                    </CardContent>
                </Card>
            ) : vista === "tabla" ? (
                <Card className="overflow-hidden py-0">
                    <Table>
                        <TableHeader>
                            <TableRow className="bg-secondary/60">
                                <TableHead>Documento</TableHead>
                                <TableHead>Tipo</TableHead>
                                <TableHead>Categoría</TableHead>
                                <TableHead>Área responsable</TableHead>
                                <TableHead>Versión</TableHead>
                                <TableHead>Publicación</TableHead>
                                <TableHead>Estado</TableHead>
                                <TableHead className="text-right">Acción</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {resultados.map((d) => (
                                <TableRow key={d.id}>
                                    <TableCell className="max-w-xs">
                                        <p className="truncate font-medium">{d.nombre}</p>
                                        <p className="truncate text-xs text-muted-foreground">{d.descripcion}</p>
                                    </TableCell>
                                    <TableCell className="text-sm">{nombreTipo(d.tipoId)}</TableCell>
                                    <TableCell className="text-sm">{nombreCategoria(d.categoriaId)}</TableCell>
                                    <TableCell className="text-sm">{nombreArea(d.areaId)}</TableCell>
                                    <TableCell className="text-sm font-medium">v{d.version}</TableCell>
                                    <TableCell className="text-sm">{d.fechaPublicacion}</TableCell>
                                    <TableCell>
                                        <EstadoBadge estado={d.estado} />
                                    </TableCell>
                                    <TableCell className="text-right">
                                        <Button asChild size="sm" variant="outline" className="gap-1.5">
                                            <Link to="/app/documento/$id" params={{ id: d.id }}>
                                                <Eye className="size-4" /> Ver detalles
                                            </Link>
                                        </Button>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </Card>
            ) : (
                <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                    {resultados.map((d) => (
                        <Card key={d.id}>
                            <CardContent className="space-y-3 py-5">
                                <div className="flex items-start justify-between gap-2">
                                    <p className="font-medium leading-snug">{d.nombre}</p>
                                    <EstadoBadge estado={d.estado} />
                                </div>
                                <p className="line-clamp-2 text-sm text-muted-foreground">{d.descripcion}</p>
                                <dl className="grid grid-cols-2 gap-2 text-xs">
                                    <Dato k="Tipo" v={nombreTipo(d.tipoId)} />
                                    <Dato k="Categoría" v={nombreCategoria(d.categoriaId)} />
                                    <Dato k="Área" v={nombreArea(d.areaId)} />
                                    <Dato k="Versión" v={`v${d.version}`} />
                                    <Dato k="Publicación" v={d.fechaPublicacion} />
                                </dl>
                                <Button asChild variant="outline" size="sm" className="w-full gap-1.5">
                                    <Link to="/app/documento/$id" params={{ id: d.id }}>
                                        <Eye className="size-4" /> Ver detalles
                                    </Link>
                                </Button>
                            </CardContent>
                        </Card>
                    ))}
                </div>
            )}
        </AppShell>
    );
}

function Dato({ k, v }: { k: string; v: string }) {
    return (
        <div>
            <dt className="text-muted-foreground">{k}</dt>
            <dd className="font-medium">{v}</dd>
        </div>
    );
}

function Filtro({
                    label,
                    value,
                    onChange,
                    opciones,
                }: {
    label: string;
    value: string;
    onChange: (v: string) => void;
    opciones: { v: string; l: string }[];
}) {
    return (
        <div className="space-y-1.5">
            <Label className="text-xs text-muted-foreground">{label}</Label>
            <Select value={value} onValueChange={onChange}>
                <SelectTrigger className="w-full">
                    <SelectValue />
                </SelectTrigger>
                <SelectContent>
                    <SelectItem value={TODOS}>Todos</SelectItem>
                    {opciones.map((o) => (
                        <SelectItem key={o.v} value={o.v}>
                            {o.l}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </div>
    );
}
