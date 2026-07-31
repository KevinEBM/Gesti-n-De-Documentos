import { Badge } from "@/components/ui/badge";
import type { Estado } from "@/lib/data";
import { cn } from "@/lib/utils";

const estilos: Record<Estado, string> = {
    publicado: "border-success/30 bg-success/12 text-success",
    borrador: "border-warning/40 bg-warning/15 text-warning-foreground",
    inactivo: "border-border bg-muted text-muted-foreground",
};

const etiquetas: Record<Estado, string> = {
    publicado: "Publicado",
    borrador: "Borrador",
    inactivo: "Inactivo",
};

export function EstadoBadge({ estado, className }: { estado: Estado; className?: string }) {
    return (
        <Badge variant="outline" className={cn("gap-1.5 font-medium", estilos[estado], className)}>
            <span className="size-1.5 rounded-full bg-current" />
            {etiquetas[estado]}
        </Badge>
    );
}

export function ActivoBadge({ activo }: { activo: boolean }) {
    return (
        <Badge
            variant="outline"
            className={cn(
                "gap-1.5 font-medium",
                activo ? "border-success/30 bg-success/12 text-success" : "border-border bg-muted text-muted-foreground",
            )}
        >
            <span className="size-1.5 rounded-full bg-current" />
            {activo ? "Activo" : "Inactivo"}
        </Badge>
    );
}
