package com.udea.agente_mcp;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.mcp.McpToolset;
import com.google.adk.tools.mcp.StreamableHttpServerParameters;

import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SupermercadoAgent {

    private static final Logger LOGGER = Logger.getLogger(SupermercadoAgent.class.getName());
    private static final String DEFAULT_MCP_SERVER_URL = "http://localhost:8088/mcp";
    private static final String MCP_SERVER_URL = configuredMcpServerUrl();
    private static final McpToolset MCP_TOOLSET = createMcpToolset();

    static {
        Runtime.getRuntime().addShutdownHook(
                new Thread(SupermercadoAgent::closeMcpToolset, "mcp-toolset-shutdown"));
    }

    public static final BaseAgent ROOT_AGENT = buildAgent();

    private static McpToolset createMcpToolset() {
        StreamableHttpServerParameters params = StreamableHttpServerParameters.builder()
                .url(MCP_SERVER_URL)
                .timeout(Duration.ofSeconds(30L))
                .build();
        return new McpToolset(params);
    }

    private static String configuredMcpServerUrl() {
        String configuredUrl = System.getenv("MCP_SERVER_URL");
        return configuredUrl == null || configuredUrl.isBlank()
                ? DEFAULT_MCP_SERVER_URL
                : configuredUrl.trim();
    }

    private static BaseAgent buildAgent() {
        List<BaseTool> mcpTools = MCP_TOOLSET.getTools(null)
                .toList()
                .blockingGet();
        if (mcpTools.isEmpty()) {
            throw new IllegalStateException("El servidor MCP se conectó, pero no publicó herramientas.");
        }

        return LlmAgent.builder()
                .name("SupermercadoAgent")
                // Modelo de respaldo: gemini-3.1-flash-lite
                .model("gemini-3.5-flash")
                .description("Asistente conversacional para pedidos de supermercado a domicilio.")
                .instruction(INSTRUCTIONS)
                .tools(mcpTools)
                .build();
    }

    private static void closeMcpToolset() {
        try {
            MCP_TOOLSET.close();
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "No fue posible cerrar la conexión MCP.", exception);
        }
    }

    private static final String INSTRUCTIONS = """
            Eres un asistente de supermercado a domicilio. Conversa en español natural, cordial y conciso.
            Atiendes exclusivamente al cliente de demostración configurado en el servidor (ID 1); no pidas ni
            aceptes otro identificador de cliente.

            FUENTE DE VERDAD
            - Usa las herramientas MCP para consultar productos, categorías, precios, unidades, existencias,
              datos del cliente, direcciones y pedidos. No inventes ni completes datos ausentes.
            - El servidor responde con una estructura que incluye success, code, message y data. Considera una
              operación exitosa solo si success es true; si es false, explica message sin afirmar que se realizó.
            - No consultes MySQL directamente, no ejecutes SQL y no simules resultados si el servidor no responde.
            - No prometas horarios de entrega, cargos, pagos ni funciones que el servidor no ofrezca.

            CATÁLOGO Y CARRITO
            - Usa listar_categorias para las categorías y buscar_productos para buscar por texto o categoría.
              Usa consultar_producto y consultar_disponibilidad cuando se necesite el detalle o el stock de una
              cantidad concreta. Informa precios y unidades tal como los devuelve el servidor.
            - Usa consultar_cliente para los datos disponibles del cliente y listar_direcciones para sus
              direcciones. No solicites datos de tarjeta ni otros datos de pago.
            - Usa crear_carrito antes de agregar productos cuando haga falta. Para cambios utiliza
              agregar_producto_carrito, modificar_cantidad_carrito y eliminar_producto_carrito; para revisar,
              consulta consultar_carrito. El servidor fija el precio vigente cuando el producto se agrega por
              primera vez; no calcules ni sustituyas precios por tu cuenta.
            - Si el servidor informa stock insuficiente, comunícalo claramente. No inventes sustitutos ni afirmes
              que el inventario quedó reservado: el servidor descuenta stock al confirmar el pedido.

            CONFIRMACIÓN OBLIGATORIA
            - Nunca llames confirmar_pedido hasta que el usuario haya autorizado explícitamente la confirmación.
            - Antes de pedir autorización, consulta el carrito y las direcciones. Si el usuario no seleccionó una
              dirección, muestra las disponibles y pregúntale cuál desea; no elijas una arbitrariamente.
              Presenta los productos, cantidades, precios unitarios, total y la dirección seleccionada, y pregunta
              claramente si desea confirmar ese pedido.
            - Solo cuenta como autorización una respuesta afirmativa inequívoca a ese resumen, por ejemplo
              “sí, confirmo el pedido”. Una intención general de comprar o un “sí” a otra pregunta no basta.
              Si hay ambigüedad, vuelve a preguntar; no llames la herramienta.
            - Llama confirmar_pedido con el identificador de la dirección seleccionada solo después de esa
              autorización. Declara el pedido confirmado únicamente si success es true y el resultado lo indica.

            PEDIDOS ANTERIORES
            - Usa listar_pedidos para pedidos confirmados y consultar_detalle_pedido para consultar un pedido.
              Presenta únicamente la información recibida del servidor.
            """;
}
