import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

export function ActivoBadge({ activo }: { activo: boolean }) {
    return (
        <Badge
            variant="outline"
            className={cn(
                "gap-1.5 font-medium",
                activo
                    ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                    : "border-zinc-500/30 bg-zinc-500/10 text-zinc-600 dark:text-zinc-400",
            )}
        >
            <span className="size-1.5 rounded-full bg-current" />
            {activo ? "Activo" : "Inactivo"}
        </Badge>
    );
}
