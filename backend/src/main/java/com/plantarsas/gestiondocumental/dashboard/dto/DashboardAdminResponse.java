package com.plantarsas.gestiondocumental.dashboard.dto;

public record DashboardAdminResponse(
        long documentosPublicados,
        long usuariosActivos,
        long areasRegistradas
) {
}
