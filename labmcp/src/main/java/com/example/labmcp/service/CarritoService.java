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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CarritoService {

    private static final BigDecimal MAXIMO_IMPORTE = new BigDecimal("99999999.99");

    private final DemoClientProperties demoClientProperties;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;

    public CarritoService(DemoClientProperties demoClientProperties,
                          ClienteRepository clienteRepository,
                          ProductoRepository productoRepository,
                          PedidoRepository pedidoRepository,
                          DetallePedidoRepository detallePedidoRepository) {
        this.demoClientProperties = demoClientProperties;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
    }

    @Transactional
    public CarritoDto crearCarrito() {
        Cliente cliente = bloquearClienteDemo();
        Pedido carrito = obtenerOCrearCarritoBloqueado(cliente);
        return representarCarrito(carrito);
    }

    @Transactional
    public CarritoDto agregarProducto(Long productoId, Integer cantidad) {
        validarProductoId(productoId);
        CatalogoService.validarCantidad(cantidad);
        Cliente cliente = bloquearClienteDemo();
        Pedido carrito = obtenerOCrearCarritoBloqueado(cliente);
        Producto producto = productoRepository.findLockedById(productoId)
                .orElseThrow(() -> new BusinessException("PRODUCTO_NO_ENCONTRADO",
                        "No se encontró el producto solicitado."));
        validarProductoActivo(producto);

        DetallePedido detalle = detallePedidoRepository
                .findByPedido_IdAndProducto_Id(carrito.getId(), productoId)
                .orElse(null);
        long cantidadFinal = (detalle == null ? 0L : detalle.getCantidad()) + cantidad;
        int stockDisponible = producto.getStock() == null ? 0 : producto.getStock();
        if (cantidadFinal > stockDisponible) {
            throw new BusinessException("STOCK_INSUFICIENTE",
                    "La cantidad solicitada supera el stock disponible.");
        }

        BigDecimal precioFijado = detalle == null ? producto.getPrecio() : detalle.getPrecioUnitario();
        validarPrecio(precioFijado);
        BigDecimal subtotal = calcularSubtotal(precioFijado, (int) cantidadFinal);
        if (detalle == null) {
            detalle = new DetallePedido(carrito, producto, (int) cantidadFinal, precioFijado, subtotal);
        } else {
            detalle.setCantidad((int) cantidadFinal);
            detalle.setSubtotal(subtotal);
        }
        detallePedidoRepository.save(detalle);
        return guardarTotalYRepresentar(carrito);
    }

    @Transactional
    public CarritoDto modificarCantidad(Long productoId, Integer cantidad) {
        validarProductoId(productoId);
        CatalogoService.validarCantidad(cantidad);
        Cliente cliente = bloquearClienteDemo();
        Pedido carrito = obtenerCarritoBloqueado(cliente.getId());
        Producto producto = productoRepository.findLockedById(productoId)
                .orElseThrow(() -> new BusinessException("PRODUCTO_NO_ENCONTRADO",
                        "No se encontró el producto solicitado."));
        validarProductoActivo(producto);
        int stockDisponible = producto.getStock() == null ? 0 : producto.getStock();
        if (cantidad > stockDisponible) {
            throw new BusinessException("STOCK_INSUFICIENTE",
                    "La cantidad solicitada supera el stock disponible.");
        }

        DetallePedido detalle = detallePedidoRepository
                .findByPedido_IdAndProducto_Id(carrito.getId(), productoId)
                .orElseThrow(() -> new BusinessException("PRODUCTO_NO_EN_CARRITO",
                        "El producto no está en el carrito activo."));
        detalle.setCantidad(cantidad);
        detalle.setSubtotal(calcularSubtotal(detalle.getPrecioUnitario(), cantidad));
        detallePedidoRepository.save(detalle);
        return guardarTotalYRepresentar(carrito);
    }

    @Transactional
    public CarritoDto eliminarProducto(Long productoId) {
        validarProductoId(productoId);
        Cliente cliente = bloquearClienteDemo();
        Pedido carrito = obtenerCarritoBloqueado(cliente.getId());
        DetallePedido detalle = detallePedidoRepository
                .findByPedido_IdAndProducto_Id(carrito.getId(), productoId)
                .orElseThrow(() -> new BusinessException("PRODUCTO_NO_EN_CARRITO",
                        "El producto no está en el carrito activo."));
        detallePedidoRepository.delete(detalle);
        return guardarTotalYRepresentar(carrito);
    }

    @Transactional(readOnly = true)
    public CarritoDto consultarCarrito() {
        Long clienteId = demoClientProperties.clientId();
        if (!clienteRepository.existsById(clienteId)) {
            throw new BusinessException("CLIENTE_DEMO_NO_ENCONTRADO",
                    "No se encontró el cliente de demostración configurado.");
        }
        List<Pedido> carritos = pedidoRepository.findCartsByClienteId(clienteId);
        if (carritos.size() > 1) {
            throw new BusinessException("CARRITOS_DUPLICADOS",
                    "Se detectó más de un carrito activo para el cliente de demostración.");
        }
        if (carritos.isEmpty()) {
            return new CarritoDto(null, "sin_carrito", List.of(), BigDecimal.ZERO.setScale(2));
        }
        return representarCarrito(carritos.get(0));
    }

    private Cliente bloquearClienteDemo() {
        return clienteRepository.findLockedById(demoClientProperties.clientId())
                .orElseThrow(() -> new BusinessException("CLIENTE_DEMO_NO_ENCONTRADO",
                        "No se encontró el cliente de demostración configurado."));
    }

    private Pedido obtenerOCrearCarritoBloqueado(Cliente cliente) {
        List<Pedido> carritos = pedidoRepository.findCartsLockedByClienteId(cliente.getId());
        if (carritos.size() > 1) {
            throw new BusinessException("CARRITOS_DUPLICADOS",
                    "Se detectó más de un carrito activo para el cliente de demostración.");
        }
        if (!carritos.isEmpty()) {
            return carritos.get(0);
        }
        return pedidoRepository.save(new Pedido(
                cliente, LocalDateTime.now(), BigDecimal.ZERO.setScale(2), Pedido.ESTADO_CARRITO));
    }

    private Pedido obtenerCarritoBloqueado(Long clienteId) {
        List<Pedido> carritos = pedidoRepository.findCartsLockedByClienteId(clienteId);
        if (carritos.isEmpty()) {
            throw new BusinessException("CARRITO_NO_ENCONTRADO", "No hay un carrito activo.");
        }
        if (carritos.size() > 1) {
            throw new BusinessException("CARRITOS_DUPLICADOS",
                    "Se detectó más de un carrito activo para el cliente de demostración.");
        }
        return carritos.get(0);
    }

    private CarritoDto guardarTotalYRepresentar(Pedido carrito) {
        List<DetallePedido> detalles =
                detallePedidoRepository.findAllByPedido_IdOrderByIdAsc(carrito.getId());
        carrito.setTotal(calcularTotal(detalles));
        pedidoRepository.save(carrito);
        return SupermercadoMapper.carrito(carrito, detalles, carrito.getTotal());
    }

    private CarritoDto representarCarrito(Pedido carrito) {
        List<DetallePedido> detalles =
                detallePedidoRepository.findAllByPedido_IdOrderByIdAsc(carrito.getId());
        BigDecimal total = calcularTotal(detalles);
        return SupermercadoMapper.carrito(carrito, detalles, total);
    }

    static BigDecimal calcularTotal(List<DetallePedido> detalles) {
        BigDecimal total = detalles.stream()
                .map(DetallePedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        validarImporte(total);
        return total;
    }

    static BigDecimal calcularSubtotal(BigDecimal precio, int cantidad) {
        BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(cantidad))
                .setScale(2, RoundingMode.HALF_UP);
        validarImporte(subtotal);
        return subtotal;
    }

    private static void validarProductoId(Long productoId) {
        if (productoId == null || productoId <= 0) {
            throw new BusinessException("PRODUCTO_ID_INVALIDO", "El identificador del producto debe ser positivo.");
        }
    }

    private static void validarProductoActivo(Producto producto) {
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new BusinessException("PRODUCTO_INACTIVO", "El producto no está disponible para compra.");
        }
    }

    private static void validarPrecio(BigDecimal precio) {
        if (precio == null || precio.signum() < 0) {
            throw new BusinessException("PRECIO_INVALIDO",
                    "El precio del producto no es válido en el catálogo.");
        }
    }

    private static void validarImporte(BigDecimal importe) {
        if (importe.signum() < 0 || importe.compareTo(MAXIMO_IMPORTE) > 0) {
            throw new BusinessException("IMPORTE_SUPERADO",
                    "El importe supera el máximo permitido por el sistema.");
        }
    }
}
