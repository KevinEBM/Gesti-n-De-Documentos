import type { LoginResponseDto, PerfilUsuarioResponseDto } from "./api";
import type { Rol, Usuario } from "./data";

const AUTH_SESSION_KEY = "intranet.auth.session";

function getStorage(): Storage | null {
    return typeof window !== "undefined" ? window.sessionStorage : null;
}

export interface AuthSession {
    token: string;
    tipo: string;
    usuario: Usuario;
}

const ROL_BACKEND_TO_FRONTEND: Record<string, Rol> = {
    ADMINISTRADOR: "administrador",
    JEFE_AREA: "jefe_area",
    ADMINISTRATIVO: "administrativo",
};

export function mapRolBackend(rol: string): Rol {
    const mapeado = ROL_BACKEND_TO_FRONTEND[rol];
    if (!mapeado) {
        throw new Error(`Rol backend no reconocido: ${rol}`);
    }
    return mapeado;
}

export function perfilResponseToUsuario(datos: PerfilUsuarioResponseDto): Usuario {
    return {
        id: String(datos.id),
        nombre: `${datos.nombres} ${datos.apellidos}`.trim(),
        correo: datos.correo,
        rol: mapRolBackend(datos.rol),
        activo: true,
        areaId: datos.areaPrincipalId != null ? String(datos.areaPrincipalId) : undefined,
        areaPrincipalNombre: datos.areaPrincipalNombre,
    };
}

export function loginResponseToUsuario(datos: LoginResponseDto): Usuario {
    return perfilResponseToUsuario(datos);
}

export function mismoUsuarioSesion(a: Usuario, b: Usuario): boolean {
    return (
        a.id === b.id &&
        a.nombre === b.nombre &&
        a.correo === b.correo &&
        a.rol === b.rol &&
        a.activo === b.activo &&
        a.areaId === b.areaId &&
        a.areaPrincipalNombre === b.areaPrincipalNombre
    );
}

export function etiquetaAreaPrincipal(usuario: Usuario): string {
    const nombre = usuario.areaPrincipalNombre?.trim();
    if (nombre) return nombre;
    if (usuario.rol === "administrador") return "No aplica";
    return "Área no disponible";
}

export function patchUsuarioAreaEnSesion(
    areaId: string | undefined,
    areaPrincipalNombre: string | null | undefined,
): Usuario | null {
    const session = getSession();
    if (!session) {
        return null;
    }

    const usuarioActualizado: Usuario = {
        ...session.usuario,
        areaId,
        areaPrincipalNombre: areaPrincipalNombre ?? null,
    };

    saveSession({ ...session, usuario: usuarioActualizado });
    return usuarioActualizado;
}

export function saveSession(session: AuthSession): void {
    const storage = getStorage();
    if (!storage) return;
    storage.setItem(AUTH_SESSION_KEY, JSON.stringify(session));
}

export function getSession(): AuthSession | null {
    const storage = getStorage();
    if (!storage) return null;

    const raw = storage.getItem(AUTH_SESSION_KEY);
    if (!raw) return null;

    try {
        return JSON.parse(raw) as AuthSession;
    } catch {
        storage.removeItem(AUTH_SESSION_KEY);
        return null;
    }
}

export function getToken(): string | null {
    return getSession()?.token ?? null;
}

export function clearSession(): void {
    const storage = getStorage();
    if (!storage) return;
    storage.removeItem(AUTH_SESSION_KEY);
}
