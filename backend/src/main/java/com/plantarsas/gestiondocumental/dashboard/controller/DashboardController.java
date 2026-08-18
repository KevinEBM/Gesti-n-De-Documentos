package com.plantarsas.gestiondocumental.dashboard.controller;

import com.plantarsas.gestiondocumental.dashboard.dto.ActividadDocumentalResponse;
import com.plantarsas.gestiondocumental.dashboard.dto.DashboardAdminResponse;
import com.plantarsas.gestiondocumental.dashboard.service.DashboardService;
import com.plantarsas.gestiondocumental.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ApiResponse<DashboardAdminResponse> obtenerDashboard() {
        return ApiResponse.exitosa(dashboardService.obtenerDashboard());
    }

    @GetMapping("/actividad-reciente")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ApiResponse<List<ActividadDocumentalResponse>> obtenerActividadReciente() {
        return ApiResponse.exitosa(dashboardService.obtenerActividadReciente());
    }
}
