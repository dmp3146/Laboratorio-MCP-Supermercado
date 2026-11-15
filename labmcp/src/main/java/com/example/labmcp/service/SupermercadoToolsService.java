package com.example.labmcp.service;

import com.example.labmcp.dto.SupermercadoDtos.CarritoDto;
import com.example.labmcp.dto.SupermercadoDtos.CategoriaDto;
import com.example.labmcp.dto.SupermercadoDtos.ClienteDto;
import com.example.labmcp.dto.SupermercadoDtos.DireccionDto;
import com.example.labmcp.dto.SupermercadoDtos.DisponibilidadDto;
import com.example.labmcp.dto.SupermercadoDtos.PedidoDetalleDto;
import com.example.labmcp.dto.SupermercadoDtos.PedidoResumenDto;
import com.example.labmcp.dto.SupermercadoDtos.ProductoDto;
import com.example.labmcp.dto.ToolResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

@Service
public class SupermercadoToolsService {

    private final CatalogoService catalogoService;
    private final ClienteService clienteService;
    private final CarritoService carritoService;
    private final PedidoService pedidoService;

    public SupermercadoToolsService(CatalogoService catalogoService,
                                    ClienteService clienteService,
                                    CarritoService carritoService,
                                    PedidoService pedidoService) {
        this.catalogoService = catalogoService;
        this.clienteService = clienteService;
        this.carritoService = carritoService;
        this.pedidoService = pedidoService;
    }

    @Tool(name = "listar_categorias", description = "Lista las categorías disponibles en el catálogo.")
    public ToolResponse<List<CategoriaDto>> listarCategorias() {
        return ejecutar("Categorías consultadas.", catalogoService::listarCategorias);
    }

    @Tool(name = "buscar_productos",
            description = "Busca productos activos por nombre y/o categoría. Si no se indican filtros, muestra productos activos.")
    public ToolResponse<List<ProductoDto>> buscarProductos(
            @ToolParam(description = "Texto que se buscará en el nombre del producto.", required = false)
            String texto,
            @ToolParam(description = "Identificador de categoría para filtrar los resultados.", required = false)
            Long categoriaId,
            @ToolParam(description = "Cantidad máxima de resultados, entre 1 y 50.", required = false)
            Integer limite) {
        return ejecutar("Productos consultados.",
                () -> catalogoService.buscarProductos(texto, categoriaId, limite));
    }

    @Tool(name = "consultar_producto", description = "Obtiene la información de un producto activo por su identificador.")
    public ToolResponse<ProductoDto> consultarProducto(
            @ToolParam(description = "Identificador del producto.") Long productoId) {
        return ejecutar("Producto consultado.", () -> catalogoService.consultarProducto(productoId));
    }

    @Tool(name = "consultar_disponibilidad",
            description = "Consulta si un producto tiene stock para la cantidad solicitada.")
    public ToolResponse<DisponibilidadDto> consultarDisponibilidad(
            @ToolParam(description = "Identificador del producto.") Long productoId,
            @ToolParam(description = "Cantidad que el cliente desea comprar.") Integer cantidad) {
        return ejecutar("Disponibilidad consultada.",
                () -> catalogoService.consultarDisponibilidad(productoId, cantidad));
    }

    @Tool(name = "consultar_cliente", description = "Consulta los datos del cliente de demostración configurado.")
    public ToolResponse<ClienteDto> consultarCliente() {
        return ejecutar("Cliente consultado.", clienteService::consultarClienteDemo);
    }

    @Tool(name = "listar_direcciones",
            description = "Lista las direcciones asociadas al cliente de demostración configurado.")
    public ToolResponse<List<DireccionDto>> listarDirecciones() {
        return ejecutar("Direcciones consultadas.", clienteService::listarDirecciones);
    }

    @Tool(name = "crear_carrito", description = "Obtiene o crea el carrito activo del cliente de demostración.")
    public ToolResponse<CarritoDto> crearCarrito() {
        return ejecutar("Carrito disponible.", carritoService::crearCarrito);
    }

    @Tool(name = "agregar_producto_carrito",
            description = "Agrega una cantidad de un producto activo al carrito; fija el precio vigente al agregarlo por primera vez.")
    public ToolResponse<CarritoDto> agregarProductoCarrito(
            @ToolParam(description = "Identificador del producto.") Long productoId,
            @ToolParam(description = "Cantidad que se agregará al carrito.") Integer cantidad) {
        return ejecutar("Producto agregado al carrito.",
                () -> carritoService.agregarProducto(productoId, cantidad));
    }

    @Tool(name = "modificar_cantidad_carrito",
            description = "Reemplaza la cantidad de un producto que ya está en el carrito.")
    public ToolResponse<CarritoDto> modificarCantidadCarrito(
            @ToolParam(description = "Identificador del producto.") Long productoId,
            @ToolParam(description = "Nueva cantidad para el producto.") Integer cantidad) {
        return ejecutar("Cantidad actualizada.", () -> carritoService.modificarCantidad(productoId, cantidad));
    }

    @Tool(name = "eliminar_producto_carrito",
            description = "Elimina un producto del carrito del cliente de demostración.")
    public ToolResponse<CarritoDto> eliminarProductoCarrito(
            @ToolParam(description = "Identificador del producto.") Long productoId) {
        return ejecutar("Producto eliminado del carrito.", () -> carritoService.eliminarProducto(productoId));
    }

    @Tool(name = "consultar_carrito", description = "Consulta los productos y el total del carrito activo.")
    public ToolResponse<CarritoDto> consultarCarrito() {
        return ejecutar("Carrito consultado.", carritoService::consultarCarrito);
    }

    @Tool(name = "confirmar_pedido",
            description = "Confirma el carrito usando una dirección del cliente; valida todo el stock y lo descuenta de forma transaccional.")
    public ToolResponse<PedidoDetalleDto> confirmarPedido(
            @ToolParam(description = "Identificador de una dirección perteneciente al cliente de demostración.")
            Long direccionId) {
        return ejecutar("Pedido confirmado.", () -> pedidoService.confirmarPedido(direccionId));
    }

    @Tool(name = "listar_pedidos", description = "Lista los pedidos confirmados del cliente de demostración.")
    public ToolResponse<List<PedidoResumenDto>> listarPedidos() {
        return ejecutar("Pedidos consultados.", pedidoService::consultarPedidos);
    }

    @Tool(name = "consultar_detalle_pedido",
            description = "Consulta un pedido y sus productos, limitado al cliente de demostración.")
    public ToolResponse<PedidoDetalleDto> consultarDetallePedido(
            @ToolParam(description = "Identificador del pedido.") Long pedidoId) {
        return ejecutar("Detalle del pedido consultado.", () -> pedidoService.consultarPedido(pedidoId));
    }

    private static <T> ToolResponse<T> ejecutar(String mensajeExito, Supplier<T> operacion) {
        try {
            return ToolResponse.success(mensajeExito, operacion.get());
        } catch (BusinessException exception) {
            return ToolResponse.failure(exception.getCode(), exception.getMessage());
        }
    }
}
