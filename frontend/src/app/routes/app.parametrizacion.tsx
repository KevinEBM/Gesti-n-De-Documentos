import { createFileRoute } from "@tanstack/react-router";
import type { ComponentType } from "react";
import { Plus } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { ActivoBadge } from "@/components/EstadoBadge";
import { Button } from "@/components/ui/button";
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
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
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";
import {
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";
import { useIntranet } from "@/lib/store";

import { obtenerIconoArea } from "@/lib/iconos-areas";
import { obtenerIconoFormato } from "@/lib/iconos-formatos";
import { obtenerIconoSubProceso } from "@/lib/iconos-subprocesos";

export const Route = createFileRoute("/app/parametrizacion")({
    head: () => ({
        meta: [
            { title: "Parametrización — Intranet documental" },
            {
                name: "description",
                content:
                    "Administra áreas, subprocesos y tipos de documento de la intranet corporativa.",
            },
            {
                property: "og:title",
                content: "Parametrización — Intranet documental",
            },
            {
                property: "og:description",
                content:
                    "Áreas, subprocesos y tipos de documento configurables.",
            },
        ],
    }),
    component: Parametrizacion,
});

type Clave = "areas" | "sub_proceso" | "tipos";

interface RegistroParametro {
    id: string;
    nombre: string;
    descripcion: string;
    activo: boolean;
    areaId?: string;
}

function Parametrizacion() {
    const { areas, sub_proceso, tipos } = useIntranet();

    return (
        <AppShell
            titulo="Parametrización"
            descripcion="Áreas, subprocesos y tipos de documento"
        >
            <Tabs defaultValue="areas" className="space-y-4">
                <TabsList>
                    <TabsTrigger
                        value="areas"
                        className="flex items-center gap-2 border border-input bg-background text-black shadow-sm hover:bg-[#289248] hover:text-white hover:border-[#289248] hover:shadow data-[state=active]:bg-[#289248] data-[state=active]:text-white"
                    >
                        {areas.length > 0 &&
                            (() => {
                                const {
                                    icono: Icono,
                                    color,
                                } = obtenerIconoArea(areas[0].nombre);

                                return (
                                    <Icono
                                        className={`size-4 ${color}`}
                                    />
                                );
                            })()}
                        Áreas ({areas.length})
                    </TabsTrigger>

                    <TabsTrigger
                        value="Subproceso"
                        className="flex items-center gap-2 border border-input bg-background text-black shadow-sm hover:bg-[#289248] hover:text-white hover:border-[#289248] hover:shadow data-[state=active]:bg-[#289248] data-[state=active]:text-white"
                    >
                        {sub_proceso.length > 0 &&
                            (() => {
                                const {
                                    icono: Icono,
                                    color,
                                } = obtenerIconoSubProceso(
                                    sub_proceso[0].nombre
                                );

                                return (
                                    <Icono
                                        className={`size-4 ${color}`}
                                    />
                                );
                            })()}
                        Subproceso ({sub_proceso.length})
                    </TabsTrigger>

                    <TabsTrigger
                        value="tipos"
                        className="flex items-center gap-2 border border-input bg-background text-black shadow-sm hover:bg-[#289248] hover:text-white hover:border-[#289248] hover:shadow data-[state=active]:bg-[#289248] data-[state=active]:text-white"
                    >
                        {tipos.length > 0 &&
                            (() => {
                                const {
                                    icono: Icono,
                                    color,
                                } = obtenerIconoFormato(tipos[0].nombre);

                                return (
                                    <Icono
                                        className={`size-4 ${color}`}
                                    />
                                );
                            })()}
                        Tipos de documento ({tipos.length})
                    </TabsTrigger>
                </TabsList>

                <TabsContent value="areas">
                    <Seccion
                        clave="areas"
                        titulo="Áreas"
                        descripcion="Unidades organizacionales responsables de los documentos."
                        registros={areas}
                    />
                </TabsContent>

                <TabsContent value="Subproceso">
                    <Seccion
                        clave="sub_proceso"
                        titulo="Subprocesos"
                        descripcion="Agrupaciones temáticas para clasificar la documentación asociadas a un área."
                        registros={sub_proceso}
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
    registros: RegistroParametro[];
}) {
    const { areas, guardarParametro, alternarParametro } = useIntranet();

    const [abierto, setAbierto] = useState(false);

    const [form, setForm] = useState<{
        id?: string;
        nombre: string;
        descripcion: string;
        activo: boolean;
        areaId?: string;
    }>({
        nombre: "",
        descripcion: "",
        activo: true,
        areaId: "",
    });

    const [error, setError] = useState("");

    const guardar = () => {
        if (!form.nombre.trim()) {
            setError("El nombre es obligatorio.");
            return;
        }

        if (clave === "sub_proceso" && !form.areaId) {
            setError("Debe seleccionar un área obligatoriamente.");
            return;
        }

        guardarParametro(
            clave,
            form as {
                id: string;
                nombre: string;
                descripcion: string;
                activo: boolean;
                areaId?: string;
            }
        );

        toast.success(
            form.id ? "Registro actualizado" : "Registro creado"
        );

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
                        setForm({
                            nombre: "",
                            descripcion: "",
                            activo: true,
                            areaId: "",
                        });
                        setError("");
                        setAbierto(true);
                    }}
                >
                    <Plus className="size-4" />
                    Nuevo
                </Button>
            </CardHeader>

            <CardContent className="px-0 pb-0">
                <Table>
                    <TableHeader>
                        <TableRow className="bg-secondary/60">
                            <TableHead>Nombre</TableHead>
                            <TableHead>Descripción</TableHead>

                            {clave === "sub_proceso" && (
                                <TableHead>Área Asociada</TableHead>
                            )}

                            <TableHead>Estado</TableHead>
                            <TableHead className="text-right">
                                Acciones
                            </TableHead>
                        </TableRow>
                    </TableHeader>

                    <TableBody>
                        {registros.map((r) => {
                            const areaAsociada = areas.find(
                                (a) => a.id === r.areaId
                            );

                            let Icono : ComponentType<any> | null = null;
                            let color = "";

                            if (clave === "areas") {
                                const resultado = obtenerIconoArea(
                                    r.nombre
                                );
                                Icono = resultado.icono;
                                color = resultado.color;
                            }

                            if (clave === "sub_proceso") {
                                const resultado =
                                    obtenerIconoSubProceso(r.nombre);
                                Icono = resultado.icono;
                                color = resultado.color;
                            }

                            if (clave === "tipos") {
                                const resultado =
                                    obtenerIconoFormato(r.nombre);
                                Icono = resultado.icono;
                                color = resultado.color;
                            }

                            return (
                                <TableRow key={r.id}>
                                    <TableCell className="font-medium">
                                        <div className="flex items-center gap-2">
                                            {Icono && (
                                                <Icono
                                                    className={`size-4 ${color}`}
                                                />
                                            )}

                                            <span>{r.nombre}</span>
                                        </div>
                                    </TableCell>

                                    <TableCell className="text-sm text-muted-foreground">
                                        {r.descripcion}
                                    </TableCell>

                                    {clave === "sub_proceso" && (
                                        <TableCell className="text-sm text-muted-foreground">
                                            {areaAsociada ? (
                                                areaAsociada.nombre
                                            ) : (
                                                <span className="text-amber-600">
                                                    Sin área
                                                </span>
                                            )}
                                        </TableCell>
                                    )}

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
                                                    if (r.id) {
                                                        alternarParametro(
                                                            clave,
                                                            r.id
                                                        );

                                                        toast.success(
                                                            r.activo
                                                                ? "Registro desactivado"
                                                                : "Registro activado"
                                                        );
                                                    }
                                                }}
                                            >
                                                {r.activo
                                                    ? "Desactivar"
                                                    : "Activar"}
                                            </Button>
                                        </div>
                                    </TableCell>
                                </TableRow>
                            );
                        })}
                    </TableBody>
                </Table>
            </CardContent>

            <Dialog open={abierto} onOpenChange={setAbierto}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>
                            {form.id
                                ? "Editar registro"
                                : "Nuevo registro"}
                        </DialogTitle>

                        <DialogDescription>
                            {titulo}
                        </DialogDescription>
                    </DialogHeader>

                    <div className="space-y-4">
                        {clave === "sub_proceso" && (
                            <div className="space-y-1.5">
                                <Label>Área responsable *</Label>

                                <Select
                                    value={form.areaId || ""}
                                    onValueChange={(value) =>
                                        setForm({
                                            ...form,
                                            areaId: value,
                                        })
                                    }
                                >
                                    <SelectTrigger>
                                        <SelectValue placeholder="Seleccione un área" />
                                    </SelectTrigger>

                                    <SelectContent>
                                        {areas.map((area) => {
                                            const {
                                                icono: Icono,
                                                color,
                                            } = obtenerIconoArea(
                                                area.nombre
                                            );

                                            return (
                                                <SelectItem
                                                    key={area.id}
                                                    value={area.id}
                                                >
                                                    <div className="flex items-center gap-2">
                                                        <Icono
                                                            className={`size-4 ${color}`}
                                                        />
                                                        <span>
                                                            {area.nombre}
                                                        </span>
                                                    </div>
                                                </SelectItem>
                                            );
                                        })}
                                    </SelectContent>
                                </Select>
                            </div>
                        )}

                        <div className="space-y-1.5">
                            <Label>Nombre</Label>

                            <Input
                                value={form.nombre}
                                onChange={(e) =>
                                    setForm({
                                        ...form,
                                        nombre: e.target.value,
                                    })
                                }
                            />
                        </div>

                        <div className="space-y-1.5">
                            <Label>Descripción</Label>

                            <Textarea
                                rows={3}
                                value={form.descripcion}
                                onChange={(e) =>
                                    setForm({
                                        ...form,
                                        descripcion: e.target.value,
                                    })
                                }
                            />
                        </div>

                        {error && (
                            <p className="text-xs text-destructive">
                                {error}
                            </p>
                        )}
                    </div>

                    <DialogFooter>
                        <Button
                            variant="outline"
                            onClick={() => setAbierto(false)}
                        >
                            Cancelar
                        </Button>

                        <Button onClick={guardar}>
                            Guardar
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </Card>
    );
}