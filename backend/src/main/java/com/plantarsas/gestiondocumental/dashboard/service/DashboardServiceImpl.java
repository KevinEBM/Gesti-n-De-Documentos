package com.plantarsas.gestiondocumental.dashboard.service;

import com.plantarsas.gestiondocumental.areas.repository.AreaRepository;
import com.plantarsas.gestiondocumental.dashboard.dto.ActividadDocumentalResponse;
import com.plantarsas.gestiondocumental.dashboard.dto.DashboardAdminResponse;
import com.plantarsas.gestiondocumental.dashboard.mapper.DashboardMapper;
import com.plantarsas.gestiondocumental.documentos.repository.DocumentoRepository;
import com.plantarsas.gestiondocumental.documentos.repository.VersionDocumentoRepository;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;
import com.plantarsas.gestiondocumental.usuarios.entity.EstadoUsuario;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DocumentoRepository documentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;
    private final VersionDocumentoRepository versionDocumentoRepository;
    private final DashboardMapper dashboardMapper;

    @Override
    @Transactional(readOnly = true)
    public DashboardAdminResponse obtenerDashboard() {
        long documentosPublicados = documentoRepository.countByEstado(DocumentoEstado.PUBLICADO);
        long usuariosActivos = usuarioRepository.countByEstado(EstadoUsuario.ACTIVO);
        long areasRegistradas = areaRepository.count();

        return new DashboardAdminResponse(documentosPublicados, usuariosActivos, areasRegistradas);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActividadDocumentalResponse> obtenerActividadReciente() {
        return versionDocumentoRepository.findTop10ByOrderByFechaPublicacionDescIdDesc().stream()
                .map(dashboardMapper::toActividad)
                .toList();
    }
}
