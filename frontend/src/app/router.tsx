import { createRouter } from "@tanstack/react-router";
import { routeTree } from "./routeTree.gen";
import type { QueryClient } from "@tanstack/react-query";

export interface RouterContext {
    queryClient: QueryClient;
}

export const router = createRouter({
    routeTree,
    context: {
        queryClient: undefined!,
    },
    defaultPreload: "intent",
    defaultPreloadStaleTime: 0,
    scrollRestoration: true,

});

// Registrar el tipo del router para autocompletado y validación de tipos estricta en toda la app
declare module "@tanstack/react-router" {
    interface Register {
        router: typeof router;
    }
}

export default router;