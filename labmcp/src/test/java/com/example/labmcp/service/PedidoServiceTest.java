package com.example.labmcp.service;

import com.example.labmcp.config.DemoClientProperties;
import com.example.labmcp.model.Cliente;
import com.example.labmcp.model.DetallePedido;
import com.example.labmcp.model.Direccion;
import com.example.labmcp.model.Pedido;
import com.example.labmcp.model.Producto;
import com.example.labmcp.repository.ClienteRepository;
import com.example.labmcp.repository.DetallePedidoRepository;
import com.example.labmcp.repository.DireccionRepository;
import com.example.labmcp.repository.PedidoRepository;
import com.example.labmcp.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private DireccionRepository direccionRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private DetallePedidoRepository detallePedidoRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private Cliente cliente;
    @Mock
    private Direccion direccion;
    @Mock
    private Producto producto;

    private PedidoService pedidoService;
    private Pedido carrito;
    private DetallePedido detalle;

    @BeforeEach
    void setUp() {
        pedidoService = new PedidoService(
                new DemoClientProperties(1L),
                clienteRepository,
                direccionRepository,
                pedidoRepository,
                detallePedidoRepository,
                productoRepository);
        carrito = new Pedido(cliente, LocalDateTime.parse("2025-01-01T10:00:00"),
                BigDecimal.ZERO.setScale(2), Pedido.ESTADO_CARRITO);
        detalle = new DetallePedido(
                carrito, producto, 2, new BigDecimal("2.50"), new BigDecimal("5.00"));

        when(clienteRepository.findLockedById(1L)).thenReturn(Optional.of(cliente));
        when(pedidoRepository.findCartsLockedByClienteId(1L)).thenReturn(List.of(carrito));
        when(direccionRepository.findByIdAndCliente_Id(4L, 1L)).thenReturn(Optional.of(direccion));
        when(detallePedidoRepository.findAllByPedido_IdOrderByIdAsc(null)).thenReturn(List.of(detalle));
        when(producto.getId()).thenReturn(7L);
        when(producto.getActivo()).thenReturn(true);
        when(productoRepository.findAllLockedByIdInOrderById(List.of(7L))).thenReturn(List.of(producto));
    }

    @Test
    void confirmarPedidoValidaStockYLoDescuentaUnaSolaVez() {
        when(producto.getStock()).thenReturn(3);

        var pedido = pedidoService.confirmarPedido(4L);

        assertEquals(Pedido.ESTADO_CONFIRMADO, pedido.estado());
        assertEquals(new BigDecimal("5.00"), pedido.total());
        verify(producto).setStock(1);
        verify(productoRepository).saveAll(Objects.requireNonNull(List.of(producto)));
        verify(pedidoRepository).save(Objects.requireNonNull(carrito));
    }

    @Test
    void stockInsuficienteNoModificaInventarioNiConfirmaElPedido() {
        when(producto.getStock()).thenReturn(1);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> pedidoService.confirmarPedido(4L));

        assertEquals("STOCK_INSUFICIENTE", exception.getCode());
        verify(producto, never()).setStock(0);
        verify(productoRepository, never()).saveAll(Objects.requireNonNull(List.of(producto)));
        verify(pedidoRepository, never()).save(Objects.requireNonNull(carrito));
    }
}
