import { Outlet, createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect } from "react";

import { useIntranet } from "@/lib/store";

export const Route = createFileRoute("/app")({
    ssr: false,
    component: AppLayout,
});

function AppLayout() {
    const { sesion } = useIntranet();
    const navigate = useNavigate();

    useEffect(() => {
        if (!sesion) navigate({ to: "/", replace: true });
    }, [sesion, navigate]);

    if (!sesion) return null;
    return <Outlet />;
}
