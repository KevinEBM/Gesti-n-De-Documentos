import { createFileRoute } from "@tanstack/react-router";
import { Plus } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { ActivoBadge } from "@/components/EstadoBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";
import { useIntranet } from "@/lib/store";

export const Route = createFileRoute("/app/parametrizacion")({
    head: () => ({
        meta: [
            { title: "Parametrización — Intranet documental" },
            {
                name: "description",
                content: "Administra áreas, categorías documentales y tipos de documento de la intranet corporativa.",
            },
            { property: "og:title", content: "Parametrización — Intranet documental" },
            { property: "og:description", content: "Áreas, categorías documentales y tipos de documento configurables." },
        ],
    }),
    component: Parametrizacion,
});

type Clave = "areas" | "categorias" | "tipos";

function Parametrizacion() {
    const { areas, categorias, tipos } = useIntranet();

    return (
        <AppShell titulo="Parametrización" descripcion="Áreas, categorías documentales y tipos de documento">
            <Tabs defaultValue="areas" className="space-y-4">
                <TabsList>
                    <TabsTrigger value="areas">Áreas ({areas.length})</TabsTrigger>
                    <TabsTrigger value="categorias">Categorías ({categorias.length})</TabsTrigger>
                    <TabsTrigger value="tipos">Tipos de documento ({tipos.length})</TabsTrigger>
                </TabsList>

                <TabsContent value="areas">
                    <Seccion
                        clave="areas"
                        titulo="Áreas"
                        descripcion="Unidades organizacionales responsables de los documentos."
                        registros={areas}
                    />
                </TabsContent>
                <TabsContent value="categorias">
                    <Seccion
                        clave="categorias"
                        titulo="Categorías documentales"
                        descripcion="Agrupaciones temáticas para clasificar la documentación."
                        registros={categorias}
                    />
                </TabsContent>
                <TabsContent value="tipos">
                    <Seccion
                        clave="tipos"
                        titulo="Tipos de documento"
                        descripcion="Plantilla, protocolo, programa, manual, política, procedimiento y otros."
                        registros={tipos}
                    />
                </TabsContent>
            </Tabs>
        </AppShell>
    );
}

function Seccion({
                     clave,
                     titulo,
                     descripcion,
                     registros,
                 }: {
    clave: Clave;
    titulo: string;
    descripcion: string;
    registros: { id: string; nombre: string; descripcion: string; activo: boolean }[];
}) {
    const { guardarParametro, alternarParametro } = useIntranet();
    const [abierto, setAbierto] = useState(false);
    const [form, setForm] = useState<{ id?: string; nombre: string; descripcion: string; activo: boolean }>({
        nombre: "",
        descripcion: "",
        activo: true,
    });
    const [error, setError] = useState("");

    const guardar = () => {
        if (!form.nombre.trim()) return setError("El nombre es obligatorio.");
        guardarParametro(clave, form);
        toast.success(form.id ? "Registro actualizado" : "Registro creado");
        setAbierto(false);
    };

    return (
        <Card>
            <CardHeader className="flex-row items-start justify-between space-y-0">
                <div>
                    <CardTitle className="text-base">{titulo}</CardTitle>
                    <CardDescription>{descripcion}</CardDescription>
                </div>
                <Button
                    size="sm"
                    className="gap-1.5"
                    onClick={() => {
                        setForm({ nombre: "", descripcion: "", activo: true });
                        setError("");
                        setAbierto(true);
                    }}
                >
                    <Plus className="size-4" /> Nuevo
                </Button>
            </CardHeader>
            <CardContent className="px-0 pb-0">
                <Table>
                    <TableHeader>
                        <TableRow className="bg-secondary/60">
                            <TableHead>Nombre</TableHead>
                            <TableHead>Descripción</TableHead>
                            <TableHead>Estado</TableHead>
                            <TableHead className="text-right">Acciones</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {registros.map((r) => (
                            <TableRow key={r.id}>
                                <TableCell className="font-medium">{r.nombre}</TableCell>
                                <TableCell className="text-sm text-muted-foreground">{r.descripcion}</TableCell>
                                <TableCell>
                                    <ActivoBadge activo={r.activo} />
                                </TableCell>
                                <TableCell>
                                    <div className="flex justify-end gap-2">
                                        <Button
                                            size="sm"
                                            variant="outline"
                                            onClick={() => {
                                                setForm({ ...r });
                                                setError("");
                                                setAbierto(true);
                                            }}
                                        >
                                            Editar
                                        </Button>
                                        <Button
                                            size="sm"
                                            variant="ghost"
                                            onClick={() => {
                                                alternarParametro(clave, r.id);
                                                toast.success(r.activo ? "Registro desactivado" : "Registro activado");
                                            }}
                                        >
                                            {r.activo ? "Desactivar" : "Activar"}
                                        </Button>
                                    </div>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </CardContent>

            <Dialog open={abierto} onOpenChange={setAbierto}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>{form.id ? "Editar registro" : "Nuevo registro"}</DialogTitle>
                        <DialogDescription>{titulo}</DialogDescription>
                    </DialogHeader>
                    <div className="space-y-4">
                        <div className="space-y-1.5">
                            <Label>Nombre</Label>
                            <Input value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} />
                            {error && <p className="text-xs text-destructive">{error}</p>}
                        </div>
                        <div className="space-y-1.5">
                            <Label>Descripción</Label>
                            <Textarea
                                rows={3}
                                value={form.descripcion}
                                onChange={(e) => setForm({ ...form, descripcion: e.target.value })}
                            />
                        </div>
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setAbierto(false)}>
                            Cancelar
                        </Button>
                        <Button onClick={guardar}>Guardar</Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </Card>
    );
}
