package com.example.labmcp.service;

import com.example.labmcp.config.DemoClientProperties;
import com.example.labmcp.dto.SupermercadoDtos.PedidoDetalleDto;
import com.example.labmcp.dto.SupermercadoDtos.PedidoResumenDto;
import com.example.labmcp.model.DetallePedido;
import com.example.labmcp.model.Direccion;
import com.example.labmcp.model.Pedido;
import com.example.labmcp.model.Producto;
import com.example.labmcp.repository.ClienteRepository;
import com.example.labmcp.repository.DetallePedidoRepository;
import com.example.labmcp.repository.DireccionRepository;
import com.example.labmcp.repository.PedidoRepository;
import com.example.labmcp.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final DemoClientProperties demoClientProperties;
    private final ClienteRepository clienteRepository;
    private final DireccionRepository direccionRepository;
    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final ProductoRepository productoRepository;

    public PedidoService(DemoClientProperties demoClientProperties,
                         ClienteRepository clienteRepository,
                         DireccionRepository direccionRepository,
                         PedidoRepository pedidoRepository,
                         DetallePedidoRepository detallePedidoRepository,
                         ProductoRepository productoRepository) {
        this.demoClientProperties = demoClientProperties;
        this.clienteRepository = clienteRepository;
        this.direccionRepository = direccionRepository;
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional
    public PedidoDetalleDto confirmarPedido(Long direccionId) {
        if (direccionId == null || direccionId <= 0) {
            throw new BusinessException("DIRECCION_ID_INVALIDO",
                    "El identificador de la dirección debe ser positivo.");
        }

        Long clienteId = demoClientProperties.clientId();
        clienteRepository.findLockedById(clienteId)
                .orElseThrow(() -> new BusinessException("CLIENTE_DEMO_NO_ENCONTRADO",
                        "No se encontró el cliente de demostración configurado."));
        List<Pedido> carritos = pedidoRepository.findCartsLockedByClienteId(clienteId);
        if (carritos.isEmpty()) {
            throw new BusinessException("CARRITO_NO_ENCONTRADO", "No hay un carrito activo para confirmar.");
        }
        if (carritos.size() > 1) {
            throw new BusinessException("CARRITOS_DUPLICADOS",
                    "Se detectó más de un carrito activo para el cliente de demostración.");
        }
        Pedido carrito = carritos.get(0);

        Direccion direccion = direccionRepository.findByIdAndCliente_Id(direccionId, clienteId)
                .orElseThrow(() -> new BusinessException("DIRECCION_NO_PERTENECE_AL_CLIENTE",
                        "La dirección no existe o no pertenece al cliente de demostración."));
        List<DetallePedido> detalles =
                detallePedidoRepository.findAllByPedido_IdOrderByIdAsc(carrito.getId());
        if (detalles.isEmpty()) {
            throw new BusinessException("CARRITO_VACIO", "No se puede confirmar un pedido vacío.");
        }

        List<Long> productoIds = detalles.stream()
                .map(detalle -> detalle.getProducto().getId())
                .distinct()
                .sorted()
                .toList();
        List<Producto> productosBloqueados =
                productoRepository.findAllLockedByIdInOrderById(productoIds);
        Map<Long, Producto> productosPorId = productosBloqueados.stream()
                .collect(Collectors.toMap(
                        producto -> Objects.requireNonNull(producto).getId(), Function.identity()));

        for (DetallePedido detalle : detalles) {
            Producto producto = productosPorId.get(detalle.getProducto().getId());
            if (producto == null) {
                throw new BusinessException("PRODUCTO_NO_ENCONTRADO",
                        "Un producto del carrito ya no está disponible en el catálogo.");
            }
            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw new BusinessException("PRODUCTO_INACTIVO",
                        "Un producto del carrito ya no está disponible para compra.");
            }
            if (producto.getStock() == null || producto.getStock() < detalle.getCantidad()) {
                throw new BusinessException("STOCK_INSUFICIENTE",
                        "No hay stock suficiente para confirmar todos los productos del carrito.");
            }
        }

        for (DetallePedido detalle : detalles) {
            Producto producto = productosPorId.get(detalle.getProducto().getId());
            producto.setStock(producto.getStock() - detalle.getCantidad());
        }

        carrito.setDireccion(direccion);
        carrito.setTotal(CarritoService.calcularTotal(detalles));
        carrito.setEstado(Pedido.ESTADO_CONFIRMADO);
        carrito.setFecha(LocalDateTime.now(ZoneId.systemDefault()));
        productoRepository.saveAll(productosBloqueados);
        pedidoRepository.save(carrito);
        return SupermercadoMapper.detalle(carrito, detalles);
    }

    @Transactional(readOnly = true)
    public List<PedidoResumenDto> consultarPedidos() {
        return pedidoRepository
                .findByCliente_IdAndEstadoOrderByFechaDescIdDesc(
                        demoClientProperties.clientId(), Pedido.ESTADO_CONFIRMADO)
                .stream()
                .map(SupermercadoMapper::resumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoDetalleDto consultarPedido(Long pedidoId) {
        if (pedidoId == null || pedidoId <= 0) {
            throw new BusinessException("PEDIDO_ID_INVALIDO", "El identificador del pedido debe ser positivo.");
        }
        Long clienteId = demoClientProperties.clientId();
        Pedido pedido = pedidoRepository.findByIdAndClienteId(pedidoId, clienteId)
                .orElseThrow(() -> new BusinessException("PEDIDO_NO_ENCONTRADO",
                        "No se encontró un pedido del cliente de demostración con ese identificador."));
        List<DetallePedido> detalles =
                detallePedidoRepository.findAllByPedido_IdOrderByIdAsc(pedido.getId());
        return SupermercadoMapper.detalle(pedido, detalles);
    }
}
