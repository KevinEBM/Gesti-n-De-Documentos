import { Badge } from "@/components/ui/badge";
import type { Estado } from "@/lib/data";
import { cn } from "@/lib/utils";

const estilos: Record<Estado, string> = {
    publicado: "border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400",
    borrador: "border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400",
    inactivo: "border-zinc-500/30 bg-zinc-500/10 text-zinc-600 dark:text-zinc-400",
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
                "gap-1.5 font-medium transition-colors",
                activo
                    ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                    : "border-rose-500/30 bg-rose-500/10 text-rose-600 dark:text-rose-400"
            )}
        >
            <span
                className={cn(
                    "size-1.5 rounded-full",
                    activo ? "bg-emerald-500" : "bg-rose-500"
                )}
            />
            {activo ? "Activo" : "Inactivo"}
        </Badge>
    );
}