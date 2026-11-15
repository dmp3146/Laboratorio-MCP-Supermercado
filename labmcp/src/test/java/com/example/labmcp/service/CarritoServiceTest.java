package com.example.labmcp.service;

import com.example.labmcp.config.DemoClientProperties;
import com.example.labmcp.dto.SupermercadoDtos.CarritoDto;
import com.example.labmcp.model.Cliente;
import com.example.labmcp.model.DetallePedido;
import com.example.labmcp.model.Pedido;
import com.example.labmcp.model.Producto;
import com.example.labmcp.repository.ClienteRepository;
import com.example.labmcp.repository.DetallePedidoRepository;
import com.example.labmcp.repository.PedidoRepository;
import com.example.labmcp.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private DetallePedidoRepository detallePedidoRepository;
    @Mock
    private Cliente cliente;
    @Mock
    private Pedido carrito;
    @Mock
    private Producto producto;

    private CarritoService carritoService;
    private final List<DetallePedido> detalles = new ArrayList<>();
    private BigDecimal totalGuardado;

    @BeforeEach
    void setUp() {
        carritoService = new CarritoService(
                new DemoClientProperties(1L),
                clienteRepository,
                productoRepository,
                pedidoRepository,
                detallePedidoRepository);
    }

    private void prepararCarritoConProducto() {
        when(cliente.getId()).thenReturn(1L);
        when(clienteRepository.findLockedById(1L)).thenReturn(Optional.of(cliente));
        when(pedidoRepository.findCartsLockedByClienteId(1L)).thenReturn(List.of(carrito));
        when(carrito.getId()).thenReturn(10L);
        lenient().when(carrito.getTotal()).thenAnswer(invocation -> totalGuardado);
        lenient().doAnswer(invocation -> {
            totalGuardado = invocation.getArgument(0);
            return null;
        }).when(carrito).setTotal(any(BigDecimal.class));
        when(productoRepository.findLockedById(7L)).thenReturn(Optional.of(producto));
        lenient().when(producto.getId()).thenReturn(7L);
        when(producto.getActivo()).thenReturn(true);
        when(producto.getStock()).thenReturn(8);
        lenient().when(producto.getPrecio()).thenReturn(new BigDecimal("2.50"));
        lenient().when(producto.getNombre()).thenReturn("Arroz");
        lenient().when(detallePedidoRepository.findAllByPedido_IdOrderByIdAsc(10L))
                .thenAnswer(invocation -> detalles);
        lenient().when(detallePedidoRepository.save(any(DetallePedido.class))).thenAnswer(invocation -> {
            DetallePedido detalle = invocation.getArgument(0);
            if (!detalles.contains(detalle)) {
                detalles.add(detalle);
            }
            return detalle;
        });
        lenient().when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void agregarProductoFijaPrecioDeBaseDeDatosYNoDescuentaInventario() {
        prepararCarritoConProducto();
        when(detallePedidoRepository.findByPedido_IdAndProducto_Id(10L, 7L))
                .thenReturn(Optional.empty());

        CarritoDto resultado = carritoService.agregarProducto(7L, 2);

        assertEquals(2, resultado.productos().get(0).cantidad());
        assertEquals(new BigDecimal("2.50"), resultado.productos().get(0).precioUnitario());
        assertEquals(new BigDecimal("5.00"), resultado.total());
        verify(productoRepository, never()).save(any(Producto.class));
    }

    @Test
    void agregarProductoMantienePrecioFijadoAunqueCambieElCatalogo() {
        prepararCarritoConProducto();
        DetallePedido existente = new DetallePedido(
                carrito, producto, 1, new BigDecimal("1.25"), new BigDecimal("1.25"));
        detalles.add(existente);
        when(detallePedidoRepository.findByPedido_IdAndProducto_Id(10L, 7L))
                .thenReturn(Optional.of(existente));

        CarritoDto resultado = carritoService.agregarProducto(7L, 2);

        assertEquals(3, resultado.productos().get(0).cantidad());
        assertEquals(new BigDecimal("1.25"), resultado.productos().get(0).precioUnitario());
        assertEquals(new BigDecimal("3.75"), resultado.total());
    }

    @Test
    void cantidadMayorQueStockNoGuardaLinea() {
        prepararCarritoConProducto();
        when(detallePedidoRepository.findByPedido_IdAndProducto_Id(10L, 7L))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class, () -> carritoService.agregarProducto(7L, 9));

        assertEquals("STOCK_INSUFICIENTE", exception.getCode());
        verify(detallePedidoRepository, never()).save(any(DetallePedido.class));
    }

    @Test
    void cantidadNoPositivaSeRechazaAntesDeAccederAlRepositorio() {
        BusinessException exception = assertThrows(
                BusinessException.class, () -> carritoService.agregarProducto(7L, 0));

        assertEquals("CANTIDAD_INVALIDA", exception.getCode());
        verify(clienteRepository, never()).findLockedById(1L);
    }
}
