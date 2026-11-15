package com.example.labmcp.service;

import com.example.labmcp.dto.SupermercadoDtos.CarritoDto;
import com.example.labmcp.dto.SupermercadoDtos.CategoriaDto;
import com.example.labmcp.dto.SupermercadoDtos.ClienteDto;
import com.example.labmcp.dto.SupermercadoDtos.DireccionDto;
import com.example.labmcp.dto.SupermercadoDtos.LineaPedidoDto;
import com.example.labmcp.dto.SupermercadoDtos.PedidoDetalleDto;
import com.example.labmcp.dto.SupermercadoDtos.PedidoResumenDto;
import com.example.labmcp.dto.SupermercadoDtos.ProductoDto;
import com.example.labmcp.model.Categoria;
import com.example.labmcp.model.Cliente;
import com.example.labmcp.model.DetallePedido;
import com.example.labmcp.model.Direccion;
import com.example.labmcp.model.Pedido;
import com.example.labmcp.model.Producto;

import java.math.BigDecimal;
import java.util.List;

final class SupermercadoMapper {

    private SupermercadoMapper() {
    }

    static CategoriaDto categoria(Categoria categoria) {
        return new CategoriaDto(categoria.getId(), categoria.getNombre(), categoria.getDescripcion());
    }

    static ProductoDto producto(Producto producto) {
        Categoria categoria = producto.getCategoria();
        return new ProductoDto(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getUnidadMedida(),
                categoria == null ? null : categoria.getId(),
                categoria == null ? null : categoria.getNombre());
    }

    static ClienteDto cliente(Cliente cliente) {
        return new ClienteDto(cliente.getId(), cliente.getNombre(), cliente.getCorreo(), cliente.getTelefono());
    }

    static DireccionDto direccion(Direccion direccion) {
        if (direccion == null) {
            return null;
        }
        return new DireccionDto(
                direccion.getId(),
                direccion.getDireccion(),
                direccion.getBarrio(),
                direccion.getCiudad(),
                direccion.getCodigoPostal(),
                direccion.getIndicaciones(),
                direccion.getPrincipal());
    }

    static LineaPedidoDto linea(DetallePedido detalle) {
        return new LineaPedidoDto(
                detalle.getProducto().getId(),
                detalle.getProducto().getNombre(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getSubtotal());
    }

    static CarritoDto carrito(Pedido pedido, List<DetallePedido> detalles, BigDecimal total) {
        return new CarritoDto(
                pedido.getId(),
                pedido.getEstado(),
                detalles.stream().map(SupermercadoMapper::linea).toList(),
                total);
    }

    static PedidoResumenDto resumen(Pedido pedido) {
        Direccion direccion = pedido.getDireccion();
        return new PedidoResumenDto(
                pedido.getId(),
                pedido.getFecha(),
                pedido.getEstado(),
                pedido.getTotal(),
                direccion == null ? null : direccion.getId());
    }

    static PedidoDetalleDto detalle(Pedido pedido, List<DetallePedido> detalles) {
        return new PedidoDetalleDto(
                pedido.getId(),
                pedido.getFecha(),
                pedido.getEstado(),
                pedido.getTotal(),
                direccion(pedido.getDireccion()),
                detalles.stream().map(SupermercadoMapper::linea).toList());
    }
}
