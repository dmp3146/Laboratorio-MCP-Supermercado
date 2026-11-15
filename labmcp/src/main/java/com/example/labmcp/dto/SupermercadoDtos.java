package com.example.labmcp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class SupermercadoDtos {

    private SupermercadoDtos() {
    }

    public record CategoriaDto(Long id, String nombre, String descripcion) {
    }

    public record ProductoDto(Long id, String codigo, String nombre, String descripcion,
                              BigDecimal precio, Integer stock, String unidadMedida,
                              Long categoriaId, String categoriaNombre) {
    }

    public record DisponibilidadDto(Long productoId, String nombre, boolean activo,
                                    Integer stockDisponible, Integer cantidadSolicitada,
                                    boolean disponible, BigDecimal precioUnitario) {
    }

    public record ClienteDto(Long id, String nombre, String correo, String telefono) {
    }

    public record DireccionDto(Long id, String direccion, String barrio, String ciudad,
                               String codigoPostal, String indicaciones, Boolean principal) {
    }

    public record LineaPedidoDto(Long productoId, String nombre, Integer cantidad,
                                 BigDecimal precioUnitario, BigDecimal subtotal) {
    }

    public record CarritoDto(Long pedidoId, String estado, List<LineaPedidoDto> productos,
                             BigDecimal total) {
        public CarritoDto {
            productos = List.copyOf(productos);
        }
    }

    public record PedidoResumenDto(Long pedidoId, LocalDateTime fecha, String estado,
                                   BigDecimal total, Long direccionId) {
    }

    public record PedidoDetalleDto(Long pedidoId, LocalDateTime fecha, String estado,
                                   BigDecimal total, DireccionDto direccion,
                                   List<LineaPedidoDto> productos) {
        public PedidoDetalleDto {
            productos = List.copyOf(productos);
        }
    }
}
