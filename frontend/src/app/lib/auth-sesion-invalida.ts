import { clearSession } from "./auth-storage";

type ListenerSesionInvalidada = () => void;

const listeners = new Set<ListenerSesionInvalidada>();

export function esPeticionLogin(path: string, method?: string): boolean {
    const metodo = (method ?? "GET").toUpperCase();
    const ruta = path.replace(/[?#].*$/, "");
    return metodo === "POST" && (ruta === "/api/auth/login" || ruta.endsWith("/api/auth/login"));
}

export function suscribirSesionInvalidada(listener: ListenerSesionInvalidada): () => void {
    listeners.add(listener);
    return () => {
        listeners.delete(listener);
    };
}

export function invalidarSesion(): void {
    clearSession();
    for (const listener of listeners) {
        listener();
    }
}

/** 401 de peticiones autenticadas. El 401 de login no invalida. El 403 no entra aquí. */
export function invalidarSesionSiNoAutorizada(
    status: number,
    path: string,
    method?: string,
): void {
    if (status !== 401) {
        return;
    }
    if (esPeticionLogin(path, method)) {
        return;
    }
    invalidarSesion();
}
