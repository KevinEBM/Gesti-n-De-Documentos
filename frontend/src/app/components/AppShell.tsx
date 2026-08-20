import logoImg from "../resources/portada-logo.png";

import { Link, useNavigate, useRouterState } from "@tanstack/react-router";
import {
    FileStack,
    Files,
    Home,
    KeyRound,
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
    { to: "/app/documentos", label: "Biblioteca de documentos", icon: Files }//,
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
    const { sesion, cerrarSesion, permisos } = useIntranet();
    const navigate = useNavigate();
    const pathname = useRouterState({ select: (s) => s.location.pathname });
    const [abierto, setAbierto] = useState(false);

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
                    "fixed inset-y-0 left-0 z-40 flex w-73 flex-col bg-[#e5e8d3] border-r-2 border-[#d59a2a] text-sidebar-foreground transition-transform lg:fixed lg:translate-x-0", abierto ? "translate-x-0" : "-translate-x-full")}
            >
                <div className="flex items-center gap-3 border-b border-sidebar-border px-5 py-4">

                    <img
                        src={logoImg}
                        alt="Logo"
                        className="h-20 w-20 object-contain"
                    />

                    <div className="min-w-0">
                        <p className="truncate text-base font-bold text-black">
                            Intranet documental
                        </p>

                    </div>

                    <button
                        className="ml-auto lg:hidden"
                        onClick={() => setAbierto(false)}
                        aria-label="Cerrar"
                    >
                        <X className="size-5"/>
                    </button>

                </div>
                <nav className="flex flex-1 flex-col gap-7 overflow-y-auto py-6">
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
                            />
                        ))}
                    </div>
                    {esAdmin && (
                        <div className="space-y-1">
                            <p className="px-3 pb-2 text-[11px] font-semibold uppercase tracking-wider text-sidebar-foreground/50">
                                Administración
                            </p>
                            {navAdmin.map((item) => (
                                <NavItem key={item.to} {...item} pathname={pathname} onClick={() => setAbierto(false)}/>
                            ))}
                        </div>
                    )}
                </nav>

                <div className="mt-auto border-t border-sidebar-border px-3 py-4">
                    <NavItem
                        to="/app/cambiar-contrasena"
                        label="Cambiar contraseña"
                        icon={KeyRound}
                        pathname={pathname}
                        onClick={() => setAbierto(false)}
                    />
                </div>
            </aside>

            <div className="flex min-w-0 flex-1 flex-col lg:ml-72">
                <header
                    className="fixed top-0 z-30 flex h-16 items-center gap-3 border-b border-[#B57F22] bg-[#e5e8d3] px-4  lg:left-72 lg:right-0 lg:px-8">
                    <Button variant="ghost" size="icon" className="lg:hidden" onClick={() => setAbierto(true)}
                            aria-label="Abrir menú">
                        <Menu className="size-5"/>
                    </Button>

                    <div className="ml-auto flex items-center gap-3">

                        <Separator orientation="vertical" className="h-8"/>

                        <div className="hidden text-right sm:block">
                            <p className="text-sm font-medium leading-tight">
                                {sesion?.nombre}
                            </p>

                            <p className="text-xs text-muted-foreground">
                                {sesion ? etiquetaRol[sesion.rol] : ""}
                            </p>
                        </div>

                        <Button
                            variant="outline"
                            size="sm"
                            onClick={salir}
                            className=" gap-2 bg-white text-black border-gray-300 shadow hover:bg-[#289248] hover:text-white hover:border-[#289248] hover:shadow-md transition-all">
                            <LogOut className="size-4" />
                            <span className="hidden sm:inline">
                                Cerrar sesión
                            </span>
                        </Button>

                    </div>
                </header>
                <main className="flex-1 px-4 pt-20 pb-7 lg:px-10 lg:pb-9">
                    <div className="mx-auto w-full max-w-screen-2xl space-y-7">
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
    icon: React.ElementType;
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
                "flex items-center gap-3 rounded-none px-6 py-2 text-sm transition-all duration-200",
                activo
                    ? "bg-white font-bold text-black shadow-sm"
                    : "text-sidebar-foreground/80 hover:bg-white/70 hover:text-black hover:font-bold",
            )}
        >
            <Icon
                className={cn(
                    "size-4 shrink-0 transition-colors duration-200",
                    activo
                        ? "text-black"
                        : "text-sidebar-foreground/80 group-hover:text-black"
                )}
            />            <span className="truncate">{label}</span>
            {badge > 0 && (
                <span className="ml-auto rounded-full bg-sidebar-primary px-1.5 py-0.5 text-[10px] font-semibold text-sidebar-primary-foreground">
          {badge}
        </span>
            )}
        </Link>
    );
}
