import { Download, Eye } from "lucide-react";
import type { ComponentType } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
    apilado = false,
    verDeshabilitado = true,
    tituloVer = "El detalle estará disponible próximamente.",
}: {
    documento: DocumentoResumen;
    descargandoId: string | null;
    onDescargar: (documento: DocumentoResumen) => void;
    apilado?: boolean;
    verDeshabilitado?: boolean;
    tituloVer?: string;
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
                disabled={verDeshabilitado}
                title={tituloVer}
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

export function DocumentoFiltroSelect({
    label,
    value,
    onChange,
    opciones,
    tipoFiltro,
    disabled = false,
    placeholder,
    todosLabel = "Todos",
}: {
    label: string;
    value: string;
    onChange: (v: string) => void;
    opciones: { v: string; l: string }[];
    tipoFiltro?: "area" | "subproceso" | "tipo";
    disabled?: boolean;
    placeholder?: string;
    todosLabel?: string;
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
                        {todosLabel}
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
