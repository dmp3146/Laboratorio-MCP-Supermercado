package com.example.labmcp.service;

import com.example.labmcp.dto.SupermercadoDtos.ProductoDto;
import com.example.labmcp.dto.ToolResponse;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SupermercadoToolsServiceTest {

    @Test
    void registraLasCatorceHerramientasMcpEsperadas() {
        SupermercadoToolsService tools = new SupermercadoToolsService(
                mock(CatalogoService.class),
                mock(ClienteService.class),
                mock(CarritoService.class),
                mock(PedidoService.class));

        ToolCallbackProvider provider = MethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build();
        Set<String> names = Arrays.stream(provider.getToolCallbacks())
                .map(callback -> callback.getToolDefinition().name())
                .collect(Collectors.toSet());

        assertEquals(14, names.size());
        assertEquals(Set.of(
                "listar_categorias",
                "buscar_productos",
                "consultar_producto",
                "consultar_disponibilidad",
                "consultar_cliente",
                "listar_direcciones",
                "crear_carrito",
                "agregar_producto_carrito",
                "modificar_cantidad_carrito",
                "eliminar_producto_carrito",
                "consultar_carrito",
                "confirmar_pedido",
                "listar_pedidos",
                "consultar_detalle_pedido"), names);
    }

    @Test
    void convierteErroresDeNegocioEnRespuestaEstructurada() {
        CatalogoService catalogoService = mock(CatalogoService.class);
        SupermercadoToolsService tools = new SupermercadoToolsService(
                catalogoService,
                mock(ClienteService.class),
                mock(CarritoService.class),
                mock(PedidoService.class));
        when(catalogoService.buscarProductos(null, null, null))
                .thenThrow(new BusinessException("LIMITE_INVALIDO", "Límite fuera de rango."));

        ToolResponse<List<ProductoDto>> response = tools.buscarProductos(null, null, null);

        assertFalse(response.success());
        assertEquals("LIMITE_INVALIDO", response.code());
        assertEquals("Límite fuera de rango.", response.message());
    }
}
