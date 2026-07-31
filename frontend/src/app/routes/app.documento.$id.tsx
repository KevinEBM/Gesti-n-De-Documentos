import { Link, createFileRoute, useNavigate, useParams } from "@tanstack/react-router";
import { ArrowLeft, Download, Eye, FileText, History, Lock } from "lucide-react";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { EstadoBadge } from "@/components/EstadoBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useIntranet } from "@/lib/store";

export const Route = createFileRoute("/app/documento/$id")({
    head: () => ({
        meta: [
            { title: "Detalle del documento — Intranet documental" },
            {
                name: "description",
                content: "Ficha completa del documento: área responsable, categoría, versión vigente e historial de versiones.",
            },
            { property: "og:title", content: "Detalle del documento — Intranet documental" },
            { property: "og:description", content: "Ficha del documento con versión vigente e historial de versiones." },
        ],
    }),
    component: DetalleDocumento,
});

function DetalleDocumento() {
    const { id } = useParams({ from: "/app/documento/$id" });
    const navigate = useNavigate();
    const { documentosVisibles, areas, nombreArea, nombreCategoria, nombreTipo, puedeVerHistorial } = useIntranet();
    const doc = documentosVisibles.find((d) => d.id === id);

    if (!doc) {
        return (
            <AppShell titulo="Documento no disponible">
                <Card>
                    <CardContent className="py-14 text-center">
                        <p className="text-sm text-muted-foreground">
                            El documento no existe, fue retirado o no está autorizado para tu área.
                        </p>
                        <Button className="mt-4" onClick={() => navigate({ to: "/app/documentos" })}>
                            Volver a la biblioteca
                        </Button>
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    const verHistorial = puedeVerHistorial(doc);
    const autorizadas = doc.visibleTodas
        ? "Todas las áreas"
        : [doc.areaId, ...doc.areasAutorizadas]
        .filter((v, i, arr) => arr.indexOf(v) === i)
        .map((a) => nombreArea(a))
        .join(", ") || "—";

    return (
        <AppShell titulo={doc.nombre} descripcion={`Versión vigente v${doc.version} · ${doc.fechaPublicacion}`}>
            <Button asChild variant="ghost" size="sm" className="-ml-2 gap-1.5">
                <Link to="/app/documentos">
                    <ArrowLeft className="size-4" /> Volver a la biblioteca
                </Link>
            </Button>

            <div className="grid gap-4 lg:grid-cols-3">
                <Card className="lg:col-span-2">
                    <CardHeader className="flex-row items-start justify-between gap-4 space-y-0">
                        <div className="flex items-start gap-3">
                            <div className="flex size-11 items-center justify-center rounded-md bg-secondary text-secondary-foreground">
                                <FileText className="size-5" />
                            </div>
                            <div>
                                <CardTitle className="text-lg leading-snug">{doc.nombre}</CardTitle>
                                <p className="mt-1 text-xs text-muted-foreground">{doc.archivo}</p>
                            </div>
                        </div>
                        <EstadoBadge estado={doc.estado} />
                    </CardHeader>
                    <CardContent className="space-y-5">
                        <p className="text-sm leading-relaxed text-muted-foreground">{doc.descripcion}</p>
                        <Separator />
                        <dl className="grid gap-4 sm:grid-cols-2">
                            <Campo k="Área responsable" v={nombreArea(doc.areaId)} />
                            <Campo k="Categoría" v={nombreCategoria(doc.categoriaId)} />
                            <Campo k="Tipo de documento" v={nombreTipo(doc.tipoId)} />
                            <Campo k="Versión vigente" v={`v${doc.version}`} />
                            <Campo k="Fecha de actualización" v={doc.fechaPublicacion} />
                            <Campo k="Publicado por" v={doc.publicadoPor} />
                            <Campo k="Áreas autorizadas" v={autorizadas} />
                            <Campo k="Total de áreas" v={doc.visibleTodas ? `${areas.length}` : `${doc.areasAutorizadas.length + 1}`} />
                        </dl>
                    </CardContent>
                </Card>

                <Card className="h-fit">
                    <CardHeader>
                        <CardTitle className="text-base">Acciones</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-2">
                        <Button
                            className="w-full justify-between"
                            onClick={() => toast.info("Vista previa simulada", { description: doc.archivo })}
                        >
                            Visualizar versión vigente <Eye className="size-4" />
                        </Button>
                        <Button
                            variant="outline"
                            className="w-full justify-between"
                            onClick={() => toast.success("Descarga iniciada (simulada)", { description: doc.archivo })}
                        >
                            Descargar <Download className="size-4" />
                        </Button>
                        <div className="rounded-md bg-secondary p-3 text-xs text-secondary-foreground">
                            {verHistorial
                                ? `${doc.versiones.length} versión(es) registradas en el historial.`
                                : "Solo tienes acceso a la versión vigente de este documento."}
                        </div>
                    </CardContent>
                </Card>
            </div>

            {verHistorial ? (
                <Card>
                    <CardHeader className="flex-row items-center gap-2 space-y-0">
                        <History className="size-4 text-muted-foreground" />
                        <CardTitle className="text-base">Historial de versiones</CardTitle>
                    </CardHeader>
                    <CardContent className="px-0 pb-0">
                        <Table>
                            <TableHeader>
                                <TableRow className="bg-secondary/60">
                                    <TableHead>Versión</TableHead>
                                    <TableHead>Fecha</TableHead>
                                    <TableHead>Publicada por</TableHead>
                                    <TableHead>Descripción de cambios</TableHead>
                                    <TableHead className="text-right">Archivo</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {doc.versiones.map((v, i) => (
                                    <TableRow key={v.numero}>
                                        <TableCell className="font-medium">
                                            v{v.numero}
                                            {i === 0 && <span className="ml-2 text-xs font-normal text-success">vigente</span>}
                                        </TableCell>
                                        <TableCell className="text-sm">{v.fecha}</TableCell>
                                        <TableCell className="text-sm">{v.autor}</TableCell>
                                        <TableCell className="text-sm text-muted-foreground">{v.notas}</TableCell>
                                        <TableCell className="text-right">
                                            <Button
                                                size="sm"
                                                variant="ghost"
                                                className="gap-1.5"
                                                onClick={() => toast.success(`Descargando v${v.numero} (simulado)`)}
                                            >
                                                <Download className="size-4" /> Descargar
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </CardContent>
                </Card>
            ) : (
                <Card>
                    <CardContent className="flex items-center gap-3 py-6">
                        <Lock className="size-4 shrink-0 text-muted-foreground" />
                        <p className="text-sm text-muted-foreground">
                            El historial de versiones anteriores está restringido. Consulta con el jefe de tu área si necesitas una
                            versión histórica.
                        </p>
                    </CardContent>
                </Card>
            )}
        </AppShell>
    );
}

function Campo({ k, v }: { k: string; v: string }) {
    return (
        <div>
            <dt className="text-xs uppercase tracking-wide text-muted-foreground">{k}</dt>
            <dd className="mt-1 text-sm font-medium">{v}</dd>
        </div>
    );
}
