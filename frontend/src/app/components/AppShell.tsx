import { Link, useNavigate, useRouterState } from "@tanstack/react-router";
import {
    Bell,
    FileStack,
    FileText,
    Files,
    Home,
    LayoutGrid,
    LogOut,
    Menu,
    Settings2,
    Upload,
    Users,
    X,
} from "lucide-react";
import { useState, type ReactNode } from "react";

import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { etiquetaRol } from "@/lib/data";
import { useIntranet } from "@/lib/store";
import { cn } from "@/lib/utils";

const navConsulta = [
    { to: "/app/inicio", label: "Inicio", icon: Home },
    { to: "/app/documentos", label: "Biblioteca de documentos", icon: Files },
    { to: "/app/notificaciones", label: "Notificaciones", icon: Bell },
];

const navAdmin = [
    { to: "/app/panel-admin", label: "Panel administrativo", icon: LayoutGrid },
    { to: "/app/publicar", label: "Publicar documento", icon: Upload },
    { to: "/app/gestion-documentos", label: "Gestión de documentos", icon: FileStack },
    { to: "/app/usuarios", label: "Usuarios", icon: Users },
    { to: "/app/parametrizacion", label: "Parametrización", icon: Settings2 },
];

export function AppShell({
                             titulo,
                             descripcion,
                             acciones,
                             children,
                         }: {
    titulo: string;
    descripcion?: string;
    acciones?: ReactNode;
    children: ReactNode;
}) {
    const { sesion, cerrarSesion, notificacionesVisibles, permisos } = useIntranet();
    const navigate = useNavigate();
    const pathname = useRouterState({ select: (s) => s.location.pathname });
    const [abierto, setAbierto] = useState(false);

    const sinLeer = notificacionesVisibles.filter((n) => !n.leida).length;
    const esAdmin = permisos.publicarDocumentos;

    const salir = () => {
        cerrarSesion();
        navigate({ to: "/" });
    };

    return (
        <div className="flex min-h-screen w-full bg-background">
            {abierto && (
                <button
                    aria-label="Cerrar menú"
                    className="fixed inset-0 z-30 bg-foreground/40 lg:hidden"
                    onClick={() => setAbierto(false)}
                />
            )}

            <aside
                className={cn(
                    "fixed inset-y-0 left-0 z-40 flex w-72 flex-col bg-sidebar text-sidebar-foreground transition-transform lg:static lg:translate-x-0",
                    abierto ? "translate-x-0" : "-translate-x-full",
                )}
            >
                <div className="flex items-center gap-3 border-b border-sidebar-border px-5 py-4">
                    <div className="flex size-9 items-center justify-center rounded-md bg-sidebar-primary text-sidebar-primary-foreground">
                        <FileText className="size-5" />
                    </div>
                    <p className="min-w-0 truncate text-sm font-semibold text-sidebar-accent-foreground">
                        Intranet documental
                    </p>
                    <button className="ml-auto lg:hidden" onClick={() => setAbierto(false)} aria-label="Cerrar">
                        <X className="size-5" />
                    </button>
                </div>

                <nav className="flex flex-1 flex-col gap-7 overflow-y-auto px-3 py-6">
                    <div className="space-y-1">
                        <p className="px-3 pb-2 text-[11px] font-semibold uppercase tracking-wider text-sidebar-foreground/50">
                            Consulta
                        </p>
                        {navConsulta.map((item) => (
                            <NavItem
                                key={item.to}
                                {...item}
                                pathname={pathname}
                                onClick={() => setAbierto(false)}
                                badge={item.to === "/app/notificaciones" ? sinLeer : 0}
                            />
                        ))}
                    </div>
                    {esAdmin && (
                        <div className="space-y-1">
                            <p className="px-3 pb-2 text-[11px] font-semibold uppercase tracking-wider text-sidebar-foreground/50">
                                Administración
                            </p>
                            {navAdmin.map((item) => (
                                <NavItem key={item.to} {...item} pathname={pathname} onClick={() => setAbierto(false)} />
                            ))}
                        </div>
                    )}
                </nav>
            </aside>

            <div className="flex min-w-0 flex-1 flex-col">
                <header className="sticky top-0 z-20 flex h-16 items-center gap-3 border-b border-border bg-surface px-4 lg:px-8">
                    <Button variant="ghost" size="icon" className="lg:hidden" onClick={() => setAbierto(true)} aria-label="Abrir menú">
                        <Menu className="size-5" />
                    </Button>
                    <p className="truncate text-sm font-semibold">Intranet documental</p>

                    <div className="ml-auto flex items-center gap-3">
                        <Button asChild variant="ghost" size="icon" className="relative" aria-label="Notificaciones">
                            <Link to="/app/notificaciones">
                                <Bell className="size-5" />
                                {sinLeer > 0 && (
                                    <span className="absolute right-1.5 top-1.5 flex size-4 items-center justify-center rounded-full bg-destructive text-[10px] font-semibold text-destructive-foreground">
                    {sinLeer}
                  </span>
                                )}
                            </Link>
                        </Button>
                        <Separator orientation="vertical" className="h-8" />
                        <div className="hidden text-right sm:block">
                            <p className="text-sm font-medium leading-tight">{sesion?.nombre}</p>
                            <p className="text-xs text-muted-foreground">{sesion ? etiquetaRol[sesion.rol] : ""}</p>
                        </div>
                        <Button variant="outline" size="sm" onClick={salir} className="gap-2">
                            <LogOut className="size-4" />
                            <span className="hidden sm:inline">Cerrar sesión</span>
                        </Button>
                    </div>
                </header>

                <main className="flex-1 px-4 py-7 lg:px-10 lg:py-9">
                    <div className="mx-auto max-w-6xl space-y-7">
                        <div className="flex flex-wrap items-end justify-between gap-3">
                            <div className="min-w-0">
                                <h1 className="text-xl font-semibold leading-tight">{titulo}</h1>
                                {descripcion && <p className="mt-1 text-sm text-muted-foreground">{descripcion}</p>}
                            </div>
                            {acciones && <div className="flex items-center gap-2">{acciones}</div>}
                        </div>
                        {children}
                    </div>
                </main>
            </div>
        </div>
    );
}

function NavItem({
                     to,
                     label,
                     icon: Icon,
                     pathname,
                     onClick,
                     badge = 0,
                 }: {
    to: string;
    label: string;
    icon: typeof Bell;
    pathname: string;
    onClick: () => void;
    badge?: number;
}) {
    const activo = pathname === to || pathname.startsWith(`${to}/`);
    return (
        <Link
            to={to}
            onClick={onClick}
            className={cn(
                "flex items-center gap-3 rounded-md px-3 py-2 text-sm transition-colors",
                activo
                    ? "bg-sidebar-accent font-medium text-sidebar-accent-foreground"
                    : "text-sidebar-foreground/80 hover:bg-sidebar-accent/60 hover:text-sidebar-accent-foreground",
            )}
        >
            <Icon className={cn("size-4 shrink-0", activo && "text-sidebar-primary")} />
            <span className="truncate">{label}</span>
            {badge > 0 && (
                <span className="ml-auto rounded-full bg-sidebar-primary px-1.5 py-0.5 text-[10px] font-semibold text-sidebar-primary-foreground">
          {badge}
        </span>
            )}
        </Link>
    );
}
