import { Download, Eye, Loader2, Pencil, Upload } from "lucide-react";
import type { ComponentType, ReactNode } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from "@/components/ui/tooltip";
import { Label } from "@/components/ui/label";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import type { DocumentoEstado, DocumentoResumen } from "@/lib/documentos-api";
import {
    SELECT_CONTENT_CLASS,
    SELECT_ITEM_CLASS,
    SELECT_TRIGGER_CLASS,
    TODOS,
    estilosEstado,
    estilosEstadoAdminSelect,
    etiquetasEstado,
} from "@/lib/documentos-consulta-shared";
import { obtenerIconoArea } from "@/lib/iconos-areas";
import { obtenerIconoFormato } from "@/lib/iconos-formatos";
import { obtenerIconoSubProceso } from "@/lib/iconos-subprocesos";
import { cn } from "@/lib/utils";

export function DocumentoEstadoBadge({ estado }: { estado: DocumentoEstado }) {
    return (
        <Badge variant="outline" className={cn("gap-1.5 font-medium", estilosEstado[estado])}>
            <span className="size-1.5 rounded-full bg-current" />
            {etiquetasEstado[estado]}
        </Badge>
    );
}

export function DocumentoAcciones({
    documento,
    descargandoId,
    onDescargar,
    onVer,
    onEditar,
    onActualizar,
    apilado = false,
    compacto = false,
    verDeshabilitado = false,
    tituloVer,
}: {
    documento: DocumentoResumen;
    descargandoId: string | null;
    onDescargar: (documento: DocumentoResumen) => void;
    onVer?: (documento: DocumentoResumen) => void;
    onEditar?: (documento: DocumentoResumen) => void;
    onActualizar?: (documento: DocumentoResumen) => void;
    apilado?: boolean;
    compacto?: boolean;
    verDeshabilitado?: boolean;
    tituloVer?: string;
}) {
    const descargando = descargandoId === documento.id;
    const verInhabilitado = verDeshabilitado || !onVer;
    const contenidoEditable = documento.estado !== "OBSOLETO";
    const puedeEditar = onEditar && contenidoEditable;
    const puedeActualizar = onActualizar && contenidoEditable;
    const etiquetaVer = tituloVer ?? "Ver documento";

    if (compacto) {
        return (
            <TooltipProvider delayDuration={300}>
                <div className="flex items-center justify-end gap-1">
                    {puedeEditar ? (
                        <AccionIconoCompacta
                            etiqueta="Editar publicación"
                            onClick={() => onEditar(documento)}
                        >
                            <Pencil className="size-4" />
                        </AccionIconoCompacta>
                    ) : null}
                    {puedeActualizar ? (
                        <AccionIconoCompacta
                            etiqueta="Publicar nueva versión"
                            onClick={() => onActualizar(documento)}
                        >
                            <Upload className="size-4" />
                        </AccionIconoCompacta>
                    ) : null}
                    <AccionIconoCompacta
                        etiqueta={etiquetaVer}
                        disabled={verInhabilitado}
                        onClick={() => onVer?.(documento)}
                    >
                        <Eye className="size-4" />
                    </AccionIconoCompacta>
                    <AccionIconoCompacta
                        etiqueta={descargando ? "Descargando documento" : "Descargar documento"}
                        disabled={descargando}
                        onClick={() => onDescargar(documento)}
                    >
                        {descargando ? (
                            <Loader2 className="size-4 animate-spin" />
                        ) : (
                            <Download className="size-4" />
                        )}
                    </AccionIconoCompacta>
                </div>
            </TooltipProvider>
        );
    }

    return (
        <div
            className={cn(
                "flex gap-2",
                apilado ? "flex-col sm:flex-row" : "flex-wrap items-center justify-end",
            )}
        >
            {puedeEditar ? (
                <Button
                    size="sm"
                    variant="outline"
                    className="gap-1.5"
                    onClick={() => onEditar(documento)}
                >
                    <Pencil className="size-4" />
                    Editar publicación
                </Button>
            ) : null}
            {puedeActualizar ? (
                <Button
                    size="sm"
                    variant="outline"
                    className="gap-1.5"
                    onClick={() => onActualizar(documento)}
                >
                    <Upload className="size-4" />
                    Actualizar documento
                </Button>
            ) : null}
            <Button
                size="sm"
                variant="outline"
                disabled={verInhabilitado}
                title={tituloVer}
                className="gap-1.5"
                onClick={() => onVer?.(documento)}
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

function AccionIconoCompacta({
    etiqueta,
    disabled = false,
    onClick,
    children,
}: {
    etiqueta: string;
    disabled?: boolean;
    onClick: () => void;
    children: ReactNode;
}) {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    type="button"
                    size="icon"
                    variant="outline"
                    className="size-8 shrink-0"
                    aria-label={etiqueta}
                    title={etiqueta}
                    disabled={disabled}
                    onClick={onClick}
                >
                    {children}
                </Button>
            </TooltipTrigger>
            <TooltipContent side="top">{etiqueta}</TooltipContent>
        </Tooltip>
    );
}

export function DocumentoFiltroSelect({
    label,
    value,
    onChange,
    opciones,
    tipoFiltro,
    disabled = false,
    placeholder,
    todosLabel = "Todos",
    ocultarTodos = false,
}: {
    label: string;
    value: string;
    onChange: (v: string) => void;
    opciones: { v: string; l: string }[];
    tipoFiltro?: "area" | "subproceso" | "tipo";
    disabled?: boolean;
    placeholder?: string;
    todosLabel?: string;
    ocultarTodos?: boolean;
}) {
    return (
        <div className="space-y-1.5">
            <Label className="text-xs text-muted-foreground">{label}</Label>
            <Select value={value} onValueChange={onChange} disabled={disabled}>
                <SelectTrigger className={SELECT_TRIGGER_CLASS}>
                    <SelectValue placeholder={placeholder} />
                </SelectTrigger>
                <SelectContent className={SELECT_CONTENT_CLASS}>
                    {!ocultarTodos ? (
                        <SelectItem value={TODOS} className={SELECT_ITEM_CLASS}>
                            {todosLabel}
                        </SelectItem>
                    ) : null}
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

type AccionEstadoDocumento = {
    destino: DocumentoEstado;
    etiqueta: string;
    requiereConfirmacion: boolean;
};

function accionesEstadoDocumento(estado: DocumentoEstado): AccionEstadoDocumento[] {
    switch (estado) {
        case "PUBLICADO":
            return [
                { destino: "INACTIVO", etiqueta: "Desactivar", requiereConfirmacion: true },
                { destino: "OBSOLETO", etiqueta: "Obsoleto", requiereConfirmacion: true },
            ];
        case "INACTIVO":
            return [
                { destino: "PUBLICADO", etiqueta: "Activar", requiereConfirmacion: false },
                { destino: "OBSOLETO", etiqueta: "Obsoleto", requiereConfirmacion: true },
            ];
        case "OBSOLETO":
            return [{ destino: "PUBLICADO", etiqueta: "Activar", requiereConfirmacion: false }];
    }
}

export function DocumentoEstadoAdminSelect({
    estado,
    deshabilitado = false,
    onSolicitarCambio,
}: {
    estado: DocumentoEstado;
    deshabilitado?: boolean;
    onSolicitarCambio: (destino: DocumentoEstado, requiereConfirmacion: boolean) => void;
}) {
    const acciones = accionesEstadoDocumento(estado);

    return (
        <Select
            value={estado}
            disabled={deshabilitado}
            onValueChange={(valor) => {
                if (valor === estado) return;
                const accion = acciones.find((item) => item.destino === valor);
                if (accion) {
                    onSolicitarCambio(accion.destino, accion.requiereConfirmacion);
                }
            }}
        >
            <SelectTrigger
                className={cn(
                    SELECT_TRIGGER_CLASS,
                    estilosEstadoAdminSelect[estado],
                    "h-8 w-[11.5rem] border text-sm font-medium",
                )}
            >
                <SelectValue>{etiquetasEstado[estado]}</SelectValue>
            </SelectTrigger>
            <SelectContent className={SELECT_CONTENT_CLASS}>
                <SelectItem
                    value={estado}
                    className={cn(SELECT_ITEM_CLASS, estilosEstadoAdminSelect[estado])}
                >
                    {etiquetasEstado[estado]}
                </SelectItem>
                {acciones.map((accion) => (
                    <SelectItem
                        key={accion.destino}
                        value={accion.destino}
                        className={SELECT_ITEM_CLASS}
                    >
                        {accion.etiqueta}
                    </SelectItem>
                ))}
            </SelectContent>
        </Select>
    );
}
