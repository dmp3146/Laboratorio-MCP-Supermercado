package com.example.labmcp.service;

import com.example.labmcp.dto.SupermercadoDtos.CategoriaDto;
import com.example.labmcp.dto.SupermercadoDtos.DisponibilidadDto;
import com.example.labmcp.dto.SupermercadoDtos.ProductoDto;
import com.example.labmcp.model.Producto;
import com.example.labmcp.repository.CategoriaRepository;
import com.example.labmcp.repository.ProductoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class CatalogoService {

    private static final int LIMITE_PREDETERMINADO = 10;
    private static final int LIMITE_MAXIMO = 50;

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CatalogoService(CategoriaRepository categoriaRepository, ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaDto> listarCategorias() {
        return categoriaRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(SupermercadoMapper::categoria)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductoDto> buscarProductos(String texto, Long categoriaId, Integer limite) {
        if (categoriaId != null && !categoriaRepository.existsById(categoriaId)) {
            throw new BusinessException("CATEGORIA_NO_ENCONTRADA", "No existe la categoría indicada.");
        }
        int resultadoLimite = limite == null ? LIMITE_PREDETERMINADO : limite;
        if (resultadoLimite < 1 || resultadoLimite > LIMITE_MAXIMO) {
            throw new BusinessException("LIMITE_INVALIDO",
                    "El límite debe estar entre 1 y " + LIMITE_MAXIMO + ".");
        }
        String filtro = texto == null || texto.isBlank() ? null : texto.trim();
        return productoRepository.buscarActivos(filtro, categoriaId, PageRequest.of(0, resultadoLimite))
                .stream()
                .map(SupermercadoMapper::producto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoDto consultarProducto(Long productoId) {
        validarProductoId(productoId);
        Producto producto = productoRepository.findById(Objects.requireNonNull(productoId))
                .orElseThrow(() -> new BusinessException("PRODUCTO_NO_ENCONTRADO",
                        "No se encontró el producto solicitado."));
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new BusinessException("PRODUCTO_INACTIVO", "El producto no está disponible para compra.");
        }
        return SupermercadoMapper.producto(producto);
    }

    @Transactional(readOnly = true)
    public DisponibilidadDto consultarDisponibilidad(Long productoId, Integer cantidad) {
        validarProductoId(productoId);
        validarCantidad(cantidad);
        Producto producto = productoRepository.findById(Objects.requireNonNull(productoId))
                .orElseThrow(() -> new BusinessException("PRODUCTO_NO_ENCONTRADO",
                        "No se encontró el producto solicitado."));
        boolean activo = Boolean.TRUE.equals(producto.getActivo());
        int stock = producto.getStock() == null ? 0 : producto.getStock();
        return new DisponibilidadDto(
                producto.getId(),
                producto.getNombre(),
                activo,
                stock,
                cantidad,
                activo && stock >= cantidad,
                producto.getPrecio());
    }

    static void validarCantidad(Integer cantidad) {
        if (cantidad == null || cantidad <= 0) {
            throw new BusinessException("CANTIDAD_INVALIDA", "La cantidad debe ser mayor que cero.");
        }
    }

    private static void validarProductoId(Long productoId) {
        if (productoId == null || productoId <= 0) {
            throw new BusinessException("PRODUCTO_ID_INVALIDO",
                    "El identificador del producto debe ser positivo.");
        }
    }
}
