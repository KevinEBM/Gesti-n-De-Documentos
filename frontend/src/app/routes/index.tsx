import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { FileText, Lock, Mail, ShieldCheck } from "lucide-react";
import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { useIntranet } from "@/lib/store";

export const Route = createFileRoute("/")({
    head: () => ({
        meta: [
            { title: "Iniciar sesión — Intranet documental Grupo Andina" },
            {
                name: "description",
                content:
                    "Acceso privado para trabajadores autorizados de Grupo Andina a la intranet de documentos internos.",
            },
            { property: "og:title", content: "Iniciar sesión — Intranet documental Grupo Andina" },
            {
                property: "og:description",
                content: "Acceso privado para trabajadores autorizados de Grupo Andina a la intranet de documentos internos.",
            },
        ],
    }),
    component: Login,
});

const demos = [
    { correo: "admin@empresa.com", password: "admin123", rol: "Administrador", detalle: "Gestión total de la intranet" },
    {
        correo: "administrativo@empresa.com",
        password: "admin123",
        rol: "Administrativo",
        detalle: "Consulta de documentos vigentes de su área",
    },
];


function Login() {
    const { iniciarSesion, sesion } = useIntranet();
    const navigate = useNavigate();
    const [correo, setCorreo] = useState("");
    const [password, setPassword] = useState("");
    const [errores, setErrores] = useState<{ correo?: string; password?: string }>({});
    const [error, setError] = useState<string | null>(null);
    const [aviso, setAviso] = useState<string | null>(null);

    useEffect(() => {
        if (sesion) {
            navigate({ to: sesion.rol === "administrador" ? "/app/panel-admin" : "/app/inicio" });
        }
    }, [sesion, navigate]);

    const enviar = (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setAviso(null);
        const nuevos: { correo?: string; password?: string } = {};
        if (!correo.trim()) nuevos.correo = "El correo institucional es obligatorio.";
        else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo.trim()))
            nuevos.correo = "Ingresa un correo válido, por ejemplo nombre@empresa.com.";
        if (!password) nuevos.password = "La contraseña es obligatoria.";
        else if (password.length < 6) nuevos.password = "La contraseña debe tener al menos 6 caracteres.";
        setErrores(nuevos);
        if (Object.keys(nuevos).length > 0) return;

        const res = iniciarSesion(correo, password);
        if (!res.ok) setError(res.error ?? "No fue posible iniciar sesión.");
    };

    const usarDemo = (d: (typeof demos)[number]) => {
        setCorreo(d.correo);
        setPassword(d.password);
        setErrores({});
        setError(null);
    };

    return (
        <div className="grid min-h-screen lg:grid-cols-[1.1fr_1fr]">
            <div className="relative hidden flex-col justify-between bg-sidebar p-12 text-sidebar-foreground lg:flex">
                <div className="flex items-center gap-3">
                    <div className="flex size-10 items-center justify-center rounded-md bg-sidebar-primary text-sidebar-primary-foreground">
                        <FileText className="size-5" />
                    </div>
                    <div>
                        <p className="font-semibold text-sidebar-accent-foreground">Grupo Andina S.A.</p>
                        <p className="text-xs text-sidebar-foreground/60">Intranet documental</p>
                    </div>
                </div>

                <div className="max-w-md space-y-5">
                    <h2 className="text-3xl font-semibold leading-tight text-sidebar-accent-foreground">
                        Un solo lugar para la documentación interna de la organización.
                    </h2>
                    <p className="text-sm leading-relaxed text-sidebar-foreground/70">
                        Consulta manuales, políticas, protocolos y procedimientos vigentes de tu área, con control de versiones y
                        notificaciones cuando se publiquen actualizaciones.
                    </p>
                    <ul className="space-y-2 text-sm text-sidebar-foreground/70">
                        <li>· Documentos organizados por área, categoría y tipo</li>
                        <li>· Historial de versiones siempre disponible</li>
                        <li>· Acceso restringido a personal autorizado</li>
                    </ul>
                </div>

                <p className="flex items-center gap-2 text-xs text-sidebar-foreground/50">
                    <ShieldCheck className="size-4" />
                    Prototipo de demostración con datos ficticios.
                </p>
            </div>

            <div className="flex items-center justify-center px-5 py-12">
                <div className="w-full max-w-sm">
                    <div className="mb-8 flex items-center gap-3 lg:hidden">
                        <div className="flex size-10 items-center justify-center rounded-md bg-primary text-primary-foreground">
                            <FileText className="size-5" />
                        </div>
                        <div>
                            <p className="font-semibold">Grupo Andina S.A.</p>
                            <p className="text-xs text-muted-foreground">Intranet documental</p>
                        </div>
                    </div>

                    <h1 className="text-2xl font-semibold">Iniciar sesión</h1>
                    <p className="mt-1.5 text-sm text-muted-foreground">
                        Usa tu correo institucional. Las cuentas son creadas por el administrador.
                    </p>

                    <form onSubmit={enviar} noValidate className="mt-7 space-y-4">
                        <div className="space-y-1.5">
                            <Label htmlFor="correo">Correo institucional</Label>
                            <div className="relative">
                                <Mail className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                                <Input
                                    id="correo"
                                    type="email"
                                    autoComplete="email"
                                    placeholder="nombre@empresa.com"
                                    className="pl-9"
                                    value={correo}
                                    onChange={(e) => setCorreo(e.target.value)}
                                    aria-invalid={!!errores.correo}
                                />
                            </div>
                            {errores.correo && <p className="text-xs text-destructive">{errores.correo}</p>}
                        </div>

                        <div className="space-y-1.5">
                            <Label htmlFor="password">Contraseña</Label>
                            <div className="relative">
                                <Lock className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                                <Input
                                    id="password"
                                    type="password"
                                    autoComplete="current-password"
                                    placeholder="••••••••"
                                    className="pl-9"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    aria-invalid={!!errores.password}
                                />
                            </div>
                            {errores.password && <p className="text-xs text-destructive">{errores.password}</p>}
                        </div>

                        {error && (
                            <Alert variant="destructive">
                                <AlertDescription>{error}</AlertDescription>
                            </Alert>
                        )}
                        {aviso && (
                            <Alert>
                                <AlertDescription>{aviso}</AlertDescription>
                            </Alert>
                        )}

                        <Button type="submit" className="w-full">
                            Iniciar sesión
                        </Button>

                        <button
                            type="button"
                            onClick={() =>
                                setAviso("Se enviaron las instrucciones de recuperación a tu correo institucional (simulado).")
                            }
                            className="block w-full text-center text-sm text-primary underline-offset-4 hover:underline"
                        >
                            ¿Olvidaste tu contraseña?
                        </button>
                    </form>

                    <div className="mt-8 rounded-lg border border-border bg-surface p-4">
                        <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                            Usuarios de demostración
                        </p>
                        <div className="mt-3 space-y-2">
                            {demos.map((d) => (
                                <button
                                    key={d.correo}
                                    type="button"
                                    onClick={() => usarDemo(d)}
                                    className="flex w-full items-center justify-between rounded-md border border-border px-3 py-2 text-left transition-colors hover:border-primary/40 hover:bg-secondary"
                                >
                  <span className="min-w-0">
                    <span className="block truncate text-sm font-medium">{d.correo}</span>
                    <span className="block truncate text-xs text-muted-foreground">
                      {d.rol} · {d.detalle}
                    </span>
                  </span>
                                    <span className="ml-3 shrink-0 text-xs font-medium text-primary">Usar</span>
                                </button>
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
