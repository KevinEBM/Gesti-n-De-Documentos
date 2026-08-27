package com.plantarsas.gestiondocumental.tiposdocumento.service;

import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.exception.ResourceNotFoundException;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoEstadoRequest;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoRequest;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoResponse;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoUpdateRequest;
import com.plantarsas.gestiondocumental.tiposdocumento.entity.TipoDocumento;
import com.plantarsas.gestiondocumental.tiposdocumento.mapper.TipoDocumentoMapper;
import com.plantarsas.gestiondocumental.tiposdocumento.repository.TipoDocumentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipoDocumentoServiceImplTest {

    @Mock
    private TipoDocumentoRepository tipoDocumentoRepository;

    @Mock
    private TipoDocumentoMapper tipoDocumentoMapper;

    private TipoDocumentoServiceImpl tipoDocumentoServiceImpl;

    @BeforeEach
    void inicializar() {
        tipoDocumentoServiceImpl = new TipoDocumentoServiceImpl(
                tipoDocumentoRepository,
                tipoDocumentoMapper
        );
    }

    private TipoDocumentoResponse respuestaDePrueba(Long id) {
        return new TipoDocumentoResponse(
                id,
                "TA",
                "Tipo de prueba",
                "Descripcion de prueba",
                true,
                null,
                null
        );
    }

    @Test
    void crear_debeCrearConNombreYDescripcionNormalizadosYRetornarRespuesta() {
        TipoDocumentoRequest request = new TipoDocumentoRequest("  ma  ", "  Tipo A  ", "  Descripcion de prueba  ");
        TipoDocumentoResponse respuestaEsperada = respuestaDePrueba(1L);

        when(tipoDocumentoRepository.existsByNombreIgnoreCase("Tipo A")).thenReturn(false);
        when(tipoDocumentoRepository.save(any(TipoDocumento.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(tipoDocumentoMapper.toResponse(any(TipoDocumento.class))).thenReturn(respuestaEsperada);

        TipoDocumentoResponse resultado = tipoDocumentoServiceImpl.crear(request);

        ArgumentCaptor<TipoDocumento> captor = ArgumentCaptor.forClass(TipoDocumento.class);
        verify(tipoDocumentoRepository).save(captor.capture());
        TipoDocumento capturado = captor.getValue();

        assertThat(capturado.getCodigo()).isEqualTo("MA");
        assertThat(capturado.getNombre()).isEqualTo("Tipo A");
        assertThat(capturado.getDescripcion()).isEqualTo("Descripcion de prueba");
        assertThat(capturado.isActivo()).isTrue();
        assertThat(resultado).isEqualTo(respuestaEsperada);

        verify(tipoDocumentoRepository).existsByNombreIgnoreCase("Tipo A");
        verify(tipoDocumentoMapper).toResponse(capturado);
        verifyNoMoreInteractions(tipoDocumentoRepository);
    }

    @Test
    void crear_debeConvertirDescripcionVaciaEnNull() {
        TipoDocumentoRequest request = new TipoDocumentoRequest("TA", "Tipo A", "   ");
        TipoDocumentoResponse respuestaEsperada = respuestaDePrueba(1L);

        when(tipoDocumentoRepository.existsByNombreIgnoreCase("Tipo A")).thenReturn(false);
        when(tipoDocumentoRepository.save(any(TipoDocumento.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(tipoDocumentoMapper.toResponse(any(TipoDocumento.class))).thenReturn(respuestaEsperada);

        tipoDocumentoServiceImpl.crear(request);

        ArgumentCaptor<TipoDocumento> captor = ArgumentCaptor.forClass(TipoDocumento.class);
        verify(tipoDocumentoRepository).save(captor.capture());
        assertThat(captor.getValue().getDescripcion()).isNull();
        verify(tipoDocumentoMapper).toResponse(captor.getValue());
    }

    @Test
    void crear_debeRechazarNombreDuplicadoIgnorandoMayusculasYEspacios() {
        TipoDocumentoRequest request = new TipoDocumentoRequest("TA", "  tipo a  ", "Descripcion");
        when(tipoDocumentoRepository.existsByNombreIgnoreCase("tipo a")).thenReturn(true);

        assertThatThrownBy(() -> tipoDocumentoServiceImpl.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ya existe un tipo de documento con el nombre 'tipo a'")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(tipoDocumentoRepository).existsByNombreIgnoreCase("tipo a");
        verify(tipoDocumentoRepository, never()).save(any());
        verifyNoMoreInteractions(tipoDocumentoRepository);
        verifyNoInteractions(tipoDocumentoMapper);
    }

    @Test
    void listar_debeMapearTodosLosTiposEnElOrdenEntregado() {
        TipoDocumento tipo1 = mock(TipoDocumento.class);
        TipoDocumento tipo2 = mock(TipoDocumento.class);
        TipoDocumentoResponse respuesta1 = respuestaDePrueba(1L);
        TipoDocumentoResponse respuesta2 = respuestaDePrueba(2L);

        when(tipoDocumentoRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(tipo1, tipo2));
        when(tipoDocumentoMapper.toResponse(tipo1)).thenReturn(respuesta1);
        when(tipoDocumentoMapper.toResponse(tipo2)).thenReturn(respuesta2);

        List<TipoDocumentoResponse> resultado = tipoDocumentoServiceImpl.listar();

        assertThat(resultado).containsExactly(respuesta1, respuesta2);
        verify(tipoDocumentoRepository).findAllByOrderByNombreAsc();
        verify(tipoDocumentoMapper).toResponse(tipo1);
        verify(tipoDocumentoMapper).toResponse(tipo2);
        verifyNoMoreInteractions(tipoDocumentoRepository);
    }

    @Test
    void listar_debeRetornarListaVaciaYNoInvocarMapperCuandoNoHayDatos() {
        when(tipoDocumentoRepository.findAllByOrderByNombreAsc()).thenReturn(List.of());

        List<TipoDocumentoResponse> resultado = tipoDocumentoServiceImpl.listar();

        assertThat(resultado).isEmpty();
        verify(tipoDocumentoRepository).findAllByOrderByNombreAsc();
        verifyNoMoreInteractions(tipoDocumentoRepository);
        verifyNoInteractions(tipoDocumentoMapper);
    }

    @Test
    void obtenerPorId_debeRetornarTipoMapeado() {
        TipoDocumento tipoDocumento = mock(TipoDocumento.class);
        TipoDocumentoResponse respuestaEsperada = respuestaDePrueba(1L);

        when(tipoDocumentoRepository.findById(1L)).thenReturn(Optional.of(tipoDocumento));
        when(tipoDocumentoMapper.toResponse(tipoDocumento)).thenReturn(respuestaEsperada);

        TipoDocumentoResponse resultado = tipoDocumentoServiceImpl.obtenerPorId(1L);

        assertThat(resultado).isEqualTo(respuestaEsperada);
        verify(tipoDocumentoRepository).findById(1L);
        verify(tipoDocumentoMapper).toResponse(tipoDocumento);
        verifyNoMoreInteractions(tipoDocumentoRepository);
    }

    @Test
    void obtenerPorId_debeLanzarResourceNotFoundSiNoExiste() {
        when(tipoDocumentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoDocumentoServiceImpl.obtenerPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No existe un tipo de documento con id 99");

        verify(tipoDocumentoRepository).findById(99L);
        verifyNoMoreInteractions(tipoDocumentoRepository);
        verifyNoInteractions(tipoDocumentoMapper);
    }

    @Test
    void actualizar_debeActualizarNombreYDescripcionConservandoElEstado() {
        TipoDocumento tipoDocumento = new TipoDocumento("NO", "Nombre original", "Descripcion original");
        TipoDocumentoUpdateRequest request = new TipoDocumentoUpdateRequest("  nn  ", "  Nombre nuevo  ", "  Descripcion nueva  ");
        TipoDocumentoResponse respuestaEsperada = respuestaDePrueba(1L);

        when(tipoDocumentoRepository.findById(1L)).thenReturn(Optional.of(tipoDocumento));
        when(tipoDocumentoRepository.existsByNombreIgnoreCaseAndIdNot("Nombre nuevo", 1L)).thenReturn(false);
        when(tipoDocumentoMapper.toResponse(tipoDocumento)).thenReturn(respuestaEsperada);

        TipoDocumentoResponse resultado = tipoDocumentoServiceImpl.actualizar(1L, request);

        assertThat(tipoDocumento.getCodigo()).isEqualTo("NN");
        assertThat(tipoDocumento.getNombre()).isEqualTo("Nombre nuevo");
        assertThat(tipoDocumento.getDescripcion()).isEqualTo("Descripcion nueva");
        assertThat(tipoDocumento.isActivo()).isTrue();
        assertThat(resultado).isEqualTo(respuestaEsperada);

        verify(tipoDocumentoRepository).findById(1L);
        verify(tipoDocumentoRepository).existsByNombreIgnoreCaseAndIdNot("Nombre nuevo", 1L);
        verify(tipoDocumentoRepository, never()).save(any());
        verify(tipoDocumentoMapper).toResponse(tipoDocumento);
        verifyNoMoreInteractions(tipoDocumentoRepository);
    }

    @Test
    void actualizar_debePermitirActualizarUnTipoInactivo() {
        TipoDocumento tipoDocumento = new TipoDocumento("NO", "Nombre original", "Descripcion original");
        tipoDocumento.desactivar();
        TipoDocumentoUpdateRequest request = new TipoDocumentoUpdateRequest("NN", "Nombre nuevo", "Descripcion nueva");
        TipoDocumentoResponse respuestaEsperada = respuestaDePrueba(1L);

        when(tipoDocumentoRepository.findById(1L)).thenReturn(Optional.of(tipoDocumento));
        when(tipoDocumentoRepository.existsByNombreIgnoreCaseAndIdNot("Nombre nuevo", 1L)).thenReturn(false);
        when(tipoDocumentoMapper.toResponse(tipoDocumento)).thenReturn(respuestaEsperada);

        TipoDocumentoResponse resultado = tipoDocumentoServiceImpl.actualizar(1L, request);

        assertThat(tipoDocumento.getNombre()).isEqualTo("Nombre nuevo");
        assertThat(tipoDocumento.isActivo()).isFalse();
        assertThat(resultado).isEqualTo(respuestaEsperada);

        verify(tipoDocumentoRepository, never()).save(any());
        verify(tipoDocumentoMapper).toResponse(tipoDocumento);
    }

    @Test
    void actualizar_debePermitirConservarElMismoNombre() {
        TipoDocumento tipoDocumento = new TipoDocumento("TA", "Tipo A", "Descripcion");
        TipoDocumentoUpdateRequest request = new TipoDocumentoUpdateRequest("TA", "Tipo A", "Descripcion actualizada");
        TipoDocumentoResponse respuestaEsperada = respuestaDePrueba(1L);

        when(tipoDocumentoRepository.findById(1L)).thenReturn(Optional.of(tipoDocumento));
        when(tipoDocumentoRepository.existsByNombreIgnoreCaseAndIdNot("Tipo A", 1L)).thenReturn(false);
        when(tipoDocumentoMapper.toResponse(tipoDocumento)).thenReturn(respuestaEsperada);

        TipoDocumentoResponse resultado = tipoDocumentoServiceImpl.actualizar(1L, request);

        assertThat(resultado).isEqualTo(respuestaEsperada);
        assertThat(tipoDocumento.getDescripcion()).isEqualTo("Descripcion actualizada");

        verify(tipoDocumentoRepository, never()).save(any());
        verify(tipoDocumentoMapper).toResponse(tipoDocumento);
    }

    @Test
    void actualizar_debeRechazarNombreDuplicado() {
        TipoDocumento tipoDocumento = mock(TipoDocumento.class);
        TipoDocumentoUpdateRequest request = new TipoDocumentoUpdateRequest("NR", "Nombre repetido", "Descripcion");

        when(tipoDocumentoRepository.findById(1L)).thenReturn(Optional.of(tipoDocumento));
        when(tipoDocumentoRepository.existsByNombreIgnoreCaseAndIdNot("Nombre repetido", 1L)).thenReturn(true);

        assertThatThrownBy(() -> tipoDocumentoServiceImpl.actualizar(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ya existe un tipo de documento con el nombre 'Nombre repetido'")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(tipoDocumento, never()).actualizarDatos(any(), any(), any());
        verify(tipoDocumentoRepository, never()).save(any());
        verifyNoInteractions(tipoDocumentoMapper);
    }

    @Test
    void actualizar_debeLanzarResourceNotFoundSiNoExisteAntesDeValidarDuplicado() {
        TipoDocumentoUpdateRequest request = new TipoDocumentoUpdateRequest("NM", "Nombre", "Descripcion");
        when(tipoDocumentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoDocumentoServiceImpl.actualizar(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No existe un tipo de documento con id 99");

        verify(tipoDocumentoRepository).findById(99L);
        verify(tipoDocumentoRepository, never()).existsByNombreIgnoreCaseAndIdNot(any(), any());
        verify(tipoDocumentoRepository, never()).save(any());
        verifyNoMoreInteractions(tipoDocumentoRepository);
        verifyNoInteractions(tipoDocumentoMapper);
    }

    @Test
    void cambiarEstado_debeActivarSinLlamarSave() {
        TipoDocumento tipoDocumento = mock(TipoDocumento.class);
        TipoDocumentoResponse respuestaEsperada = respuestaDePrueba(1L);

        when(tipoDocumentoRepository.findById(1L)).thenReturn(Optional.of(tipoDocumento));
        when(tipoDocumentoMapper.toResponse(tipoDocumento)).thenReturn(respuestaEsperada);

        TipoDocumentoResponse resultado =
                tipoDocumentoServiceImpl.cambiarEstado(1L, new TipoDocumentoEstadoRequest(true));

        verify(tipoDocumento).activar();
        verify(tipoDocumento, never()).desactivar();
        assertThat(resultado).isEqualTo(respuestaEsperada);

        verify(tipoDocumentoRepository).findById(1L);
        verify(tipoDocumentoRepository, never()).save(any());
        verify(tipoDocumentoMapper).toResponse(tipoDocumento);
        verifyNoMoreInteractions(tipoDocumentoRepository);
    }

    @Test
    void cambiarEstado_debeDesactivarSinLlamarSave() {
        TipoDocumento tipoDocumento = mock(TipoDocumento.class);
        TipoDocumentoResponse respuestaEsperada = respuestaDePrueba(1L);

        when(tipoDocumentoRepository.findById(1L)).thenReturn(Optional.of(tipoDocumento));
        when(tipoDocumentoMapper.toResponse(tipoDocumento)).thenReturn(respuestaEsperada);

        TipoDocumentoResponse resultado =
                tipoDocumentoServiceImpl.cambiarEstado(1L, new TipoDocumentoEstadoRequest(false));

        verify(tipoDocumento).desactivar();
        verify(tipoDocumento, never()).activar();
        assertThat(resultado).isEqualTo(respuestaEsperada);

        verify(tipoDocumentoRepository).findById(1L);
        verify(tipoDocumentoRepository, never()).save(any());
        verify(tipoDocumentoMapper).toResponse(tipoDocumento);
        verifyNoMoreInteractions(tipoDocumentoRepository);
    }

    @Test
    void cambiarEstado_debeLanzarResourceNotFoundSiNoExiste() {
        when(tipoDocumentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                tipoDocumentoServiceImpl.cambiarEstado(99L, new TipoDocumentoEstadoRequest(true)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No existe un tipo de documento con id 99");

        verify(tipoDocumentoRepository).findById(99L);
        verify(tipoDocumentoRepository, never()).save(any());
        verifyNoMoreInteractions(tipoDocumentoRepository);
        verifyNoInteractions(tipoDocumentoMapper);
    }

    @Test
    void listarActivos_debeMapearSoloActivosEnElOrdenEntregado() {
        TipoDocumento tipo1 = mock(TipoDocumento.class);
        TipoDocumento tipo2 = mock(TipoDocumento.class);
        TipoDocumentoResponse respuesta1 = respuestaDePrueba(1L);
        TipoDocumentoResponse respuesta2 = respuestaDePrueba(2L);

        when(tipoDocumentoRepository.findByActivoTrueOrderByNombreAsc())
                .thenReturn(List.of(tipo1, tipo2));
        when(tipoDocumentoMapper.toResponse(tipo1)).thenReturn(respuesta1);
        when(tipoDocumentoMapper.toResponse(tipo2)).thenReturn(respuesta2);

        List<TipoDocumentoResponse> resultado = tipoDocumentoServiceImpl.listarActivos();

        assertThat(resultado).containsExactly(respuesta1, respuesta2);
        verify(tipoDocumentoRepository).findByActivoTrueOrderByNombreAsc();
        verify(tipoDocumentoMapper).toResponse(tipo1);
        verify(tipoDocumentoMapper).toResponse(tipo2);
        verifyNoMoreInteractions(tipoDocumentoRepository);
    }

    @Test
    void listarActivos_debeRetornarListaVaciaYNoInvocarMapperCuandoNoHayActivos() {
        when(tipoDocumentoRepository.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of());

        List<TipoDocumentoResponse> resultado = tipoDocumentoServiceImpl.listarActivos();

        assertThat(resultado).isEmpty();
        verify(tipoDocumentoRepository).findByActivoTrueOrderByNombreAsc();
        verifyNoMoreInteractions(tipoDocumentoRepository);
        verifyNoInteractions(tipoDocumentoMapper);
    }

    @Test
    void obtenerActivoPorId_debeRetornarLaEntidadSinMapear() {
        TipoDocumento tipoDocumento = mock(TipoDocumento.class);
        when(tipoDocumento.isActivo()).thenReturn(true);
        when(tipoDocumentoRepository.findById(1L)).thenReturn(Optional.of(tipoDocumento));

        TipoDocumento resultado = tipoDocumentoServiceImpl.obtenerActivoPorId(1L);

        assertThat(resultado).isSameAs(tipoDocumento);
        verify(tipoDocumentoRepository).findById(1L);
        verifyNoMoreInteractions(tipoDocumentoRepository);
        verifyNoInteractions(tipoDocumentoMapper);
    }

    @Test
    void obtenerActivoPorId_debeRechazarTipoInactivo() {
        TipoDocumento tipoDocumento = mock(TipoDocumento.class);
        when(tipoDocumento.isActivo()).thenReturn(false);
        when(tipoDocumento.getNombre()).thenReturn("Tipo A");
        when(tipoDocumentoRepository.findById(1L)).thenReturn(Optional.of(tipoDocumento));

        assertThatThrownBy(() -> tipoDocumentoServiceImpl.obtenerActivoPorId(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El tipo de documento 'Tipo A' está inactivo y no puede utilizarse")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(tipoDocumentoRepository).findById(1L);
        verifyNoMoreInteractions(tipoDocumentoRepository);
        verifyNoInteractions(tipoDocumentoMapper);
    }

    @Test
    void obtenerActivoPorId_debeLanzarResourceNotFoundSiNoExiste() {
        when(tipoDocumentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoDocumentoServiceImpl.obtenerActivoPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No existe un tipo de documento con id 99");

        verify(tipoDocumentoRepository).findById(99L);
        verifyNoMoreInteractions(tipoDocumentoRepository);
        verifyNoInteractions(tipoDocumentoMapper);
    }

    @Test
    void crear_debePermitirElMismoCodigoEnTiposDistintos() {
        TipoDocumentoRequest plantilla = new TipoDocumentoRequest("ode", "Plantilla", null);
        TipoDocumentoRequest imagenes = new TipoDocumentoRequest("ODE", "Imágenes", null);
        TipoDocumentoResponse respuestaPlantilla = new TipoDocumentoResponse(
                1L, "ODE", "Plantilla", null, true, null, null
        );
        TipoDocumentoResponse respuestaImagenes = new TipoDocumentoResponse(
                2L, "ODE", "Imágenes", null, true, null, null
        );

        when(tipoDocumentoRepository.existsByNombreIgnoreCase("Plantilla")).thenReturn(false);
        when(tipoDocumentoRepository.existsByNombreIgnoreCase("Imágenes")).thenReturn(false);
        when(tipoDocumentoRepository.save(any(TipoDocumento.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(tipoDocumentoMapper.toResponse(any(TipoDocumento.class)))
                .thenReturn(respuestaPlantilla, respuestaImagenes);

        TipoDocumentoResponse creadoPlantilla = tipoDocumentoServiceImpl.crear(plantilla);
        TipoDocumentoResponse creadoImagenes = tipoDocumentoServiceImpl.crear(imagenes);

        ArgumentCaptor<TipoDocumento> captor = ArgumentCaptor.forClass(TipoDocumento.class);
        verify(tipoDocumentoRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(TipoDocumento::getCodigo)
                .containsExactly("ODE", "ODE");
        assertThat(captor.getAllValues())
                .extracting(TipoDocumento::getNombre)
                .containsExactly("Plantilla", "Imágenes");
        assertThat(creadoPlantilla.codigo()).isEqualTo("ODE");
        assertThat(creadoImagenes.codigo()).isEqualTo("ODE");
        assertThat(creadoPlantilla.id()).isNotEqualTo(creadoImagenes.id());
    }

    @Test
    void crear_debePermitirOdeEnPlantillaDocumentosExternosEImagenes() {
        TipoDocumentoRequest plantilla = new TipoDocumentoRequest("ODE", "Plantilla", null);
        TipoDocumentoRequest externos = new TipoDocumentoRequest("ODE", "Documentos Externos", null);
        TipoDocumentoRequest imagenes = new TipoDocumentoRequest("ODE", "Imágenes", null);
        TipoDocumentoResponse respuestaPlantilla = new TipoDocumentoResponse(
                1L, "ODE", "Plantilla", null, true, null, null
        );
        TipoDocumentoResponse respuestaExternos = new TipoDocumentoResponse(
                2L, "ODE", "Documentos Externos", null, true, null, null
        );
        TipoDocumentoResponse respuestaImagenes = new TipoDocumentoResponse(
                3L, "ODE", "Imágenes", null, true, null, null
        );

        when(tipoDocumentoRepository.existsByNombreIgnoreCase("Plantilla")).thenReturn(false);
        when(tipoDocumentoRepository.existsByNombreIgnoreCase("Documentos Externos")).thenReturn(false);
        when(tipoDocumentoRepository.existsByNombreIgnoreCase("Imágenes")).thenReturn(false);
        when(tipoDocumentoRepository.save(any(TipoDocumento.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(tipoDocumentoMapper.toResponse(any(TipoDocumento.class)))
                .thenReturn(respuestaPlantilla, respuestaExternos, respuestaImagenes);

        TipoDocumentoResponse creadoPlantilla = tipoDocumentoServiceImpl.crear(plantilla);
        TipoDocumentoResponse creadoExternos = tipoDocumentoServiceImpl.crear(externos);
        TipoDocumentoResponse creadoImagenes = tipoDocumentoServiceImpl.crear(imagenes);

        assertThat(List.of(creadoPlantilla, creadoExternos, creadoImagenes))
                .extracting(TipoDocumentoResponse::codigo)
                .containsOnly("ODE");
        assertThat(List.of(creadoPlantilla, creadoExternos, creadoImagenes))
                .extracting(TipoDocumentoResponse::id)
                .containsExactly(1L, 2L, 3L);
        assertThat(List.of(creadoPlantilla, creadoExternos, creadoImagenes))
                .extracting(TipoDocumentoResponse::nombre)
                .containsExactly("Plantilla", "Documentos Externos", "Imágenes");
    }

    @Test
    void actualizar_debePermitirCambiarElCodigoSinValidarUnicidadDeCodigo() {
        TipoDocumento tipoDocumento = new TipoDocumento("MA", "Manual", "Descripcion");
        TipoDocumentoUpdateRequest request = new TipoDocumentoUpdateRequest("mn", "Manual", "Descripcion");
        TipoDocumentoResponse respuestaEsperada = new TipoDocumentoResponse(
                1L, "MN", "Manual", "Descripcion", true, null, null
        );

        when(tipoDocumentoRepository.findById(1L)).thenReturn(Optional.of(tipoDocumento));
        when(tipoDocumentoRepository.existsByNombreIgnoreCaseAndIdNot("Manual", 1L)).thenReturn(false);
        when(tipoDocumentoMapper.toResponse(tipoDocumento)).thenReturn(respuestaEsperada);

        TipoDocumentoResponse resultado = tipoDocumentoServiceImpl.actualizar(1L, request);

        assertThat(tipoDocumento.getCodigo()).isEqualTo("MN");
        assertThat(resultado.codigo()).isEqualTo("MN");
        verify(tipoDocumentoRepository, never()).save(any());
    }
}
