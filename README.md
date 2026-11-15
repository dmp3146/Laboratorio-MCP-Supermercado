# Chatbot de pedidos de supermercado con MCP

## 1. Descripción del proyecto

El presente proyecto consiste en el desarrollo de un chatbot inteligente para la gestión de pedidos de supermercado a domicilio, integrado con una base de datos MySQL mediante el protocolo **Model Context Protocol (MCP)**.

La aplicación permite que un usuario interactúe con un supermercado utilizando lenguaje natural, solicitando información sobre categorías, productos, precios y disponibilidad de inventario. Asimismo, puede crear y administrar un carrito de compras, consultar el valor total de los productos y confirmar pedidos indicando una dirección de entrega.

La solución integra un agente de inteligencia artificial basado en Gemini, desarrollado con Google ADK para Java, y un servidor MCP implementado con Spring Boot y Spring AI. Esta integración permite que el modelo interprete las solicitudes del usuario, identifique las herramientas necesarias y solicite su ejecución al servidor, que realiza las operaciones correspondientes sobre la base de datos.

El proyecto se desarrolla mediante dos aplicaciones independientes: `labmcp`, encargado de la lógica de negocio y la comunicación con MySQL, y `agentemcp`, responsable de la interacción conversacional con el usuario.

## 2. Objetivo

Desarrollar un cliente de tipo ChatBot integrado con un servidor MCP que permita gestionar pedidos de productos de supermercado a domicilio mediante lenguaje natural, utilizando una base de datos MySQL para almacenar y consultar la información del sistema.

## 3. Arquitectura del sistema

La arquitectura se encuentra organizada en diferentes componentes que separan la interacción con el usuario, el procesamiento mediante inteligencia artificial, la ejecución de operaciones de negocio y la persistencia de información.

Los principales componentes son:

- **Usuario:** interactúa con el sistema mediante solicitudes en lenguaje natural.
- **Chatbot (ADK Web):** proporciona la interfaz web para establecer la conversación con el agente.
- **Agente de inteligencia artificial (Gemini):** interpreta las solicitudes, determina qué operaciones deben realizarse y selecciona las herramientas MCP correspondientes.
- **Servidor MCP:** expone las funcionalidades del supermercado mediante herramientas que pueden ser invocadas por el agente.
- **Servicios de negocio:** implementan las operaciones relacionadas con el catálogo, los clientes, el carrito y los pedidos.
- **Repositorios y JPA:** gestionan el acceso y la comunicación con la base de datos MySQL.
- **Base de datos:** almacena las categorías, productos, clientes, direcciones y pedidos.

La comunicación entre el agente y el servidor se realiza mediante **MCP Streamable HTTP**, utilizando el endpoint `/mcp` en el puerto 8088. La interfaz web del chatbot se ejecuta en el puerto 8080.

El funcionamiento comienza cuando el usuario realiza una solicitud. Gemini interpreta el mensaje y, cuando necesita consultar o modificar información, solicita la ejecución de una herramienta MCP. El servidor procesa la operación, accede a la base de datos y devuelve el resultado al agente, que finalmente presenta una respuesta comprensible para el usuario.

## 4. Tecnologías utilizadas

| Tecnología | Función |
|---|---|
| Java 21 | Lenguaje de programación de ambos proyectos. |
| Spring Boot 3.4.7 | Desarrollo del servidor MCP. |
| Spring AI 1.1.4 | Implementación e integración de las herramientas MCP. |
| Spring Data JPA | Persistencia y acceso a los datos. |
| Hibernate | Mapeo entre las entidades Java y las tablas de MySQL. |
| MySQL 8 | Sistema gestor de base de datos. |
| Google ADK para Java 0.6.0 | Desarrollo del agente conversacional. |
| Gemini | Modelo de inteligencia artificial encargado de interpretar las solicitudes. |
| ADK Web | Interfaz web para interactuar con el chatbot. |
| Maven Wrapper | Construcción y gestión de dependencias de los proyectos. |

## 5. Base de datos

El sistema utiliza una base de datos MySQL denominada `supermercado_mcp`, compuesta por seis tablas principales:

- **categoria:** almacena las categorías disponibles en el supermercado.
- **cliente:** contiene la información de los clientes registrados.
- **direccion:** almacena las direcciones de entrega asociadas a cada cliente.
- **producto:** contiene el catálogo de productos, incluyendo precios, disponibilidad y existencias.
- **pedido:** registra los pedidos realizados, su cliente, dirección, valor total y estado.
- **detalle_pedido:** almacena los productos incluidos en cada pedido, sus cantidades, precios unitarios y subtotales.

Las relaciones entre estas entidades permiten administrar los pedidos y mantener la correspondencia entre clientes, direcciones, productos y categorías.

La aplicación utiliza Hibernate con la configuración `ddl-auto=validate`, lo que permite verificar la correspondencia entre las entidades y el esquema existente sin modificar automáticamente la estructura de la base de datos.

Para su funcionamiento se requiere una base de datos previamente creada y compatible con las entidades del proyecto, además de los registros necesarios para el cliente de demostración.

## 6. Servidor MCP

El servidor MCP se encuentra implementado en el proyecto `labmcp`, utilizando Spring Boot y Spring AI.

Su responsabilidad principal es proporcionar las herramientas necesarias para que el agente de inteligencia artificial pueda consultar y ejecutar operaciones sobre el supermercado.

Las funcionalidades se organizan en servicios de negocio, entidades, repositorios y objetos de transferencia de datos (DTO), manteniendo una separación entre la lógica de aplicación y el acceso a la información.

El servidor expone 14 herramientas MCP agrupadas según las siguientes funcionalidades:

**Gestión del catálogo:**
- Listar categorías.
- Buscar productos.
- Consultar información de un producto.
- Consultar disponibilidad de inventario.

**Gestión de clientes y direcciones:**
- Consultar información del cliente.
- Listar direcciones de entrega.

**Gestión del carrito:**
- Crear u obtener un carrito.
- Agregar productos.
- Modificar cantidades.
- Eliminar productos.
- Consultar el contenido y el valor total.

**Gestión de pedidos:**
- Confirmar pedidos.
- Consultar pedidos confirmados.
- Consultar el detalle de un pedido.

El servidor implementa validaciones para evitar cantidades inválidas, productos inexistentes y solicitudes que superen las existencias disponibles.

La confirmación de un pedido se realiza mediante una operación transaccional que verifica la dirección de entrega, el contenido del carrito y la disponibilidad de los productos. Una vez completadas las validaciones, se actualiza el inventario y el estado del pedido.

## 7. Cliente ChatBot

El cliente conversacional se encuentra desarrollado en el proyecto `agentemcp`, utilizando Google ADK para Java.

El agente, denominado `SupermercadoAgent`, se conecta con el servidor MCP mediante un `McpToolset`, utilizando el transporte Streamable HTTP. Durante su inicialización descubre las herramientas disponibles y las pone a disposición del modelo Gemini.

El modelo principal configurado es `gemini-3.5-flash`. También se contempla `gemini-3.1-flash-lite` como alternativa de configuración manual.

El agente cuenta con instrucciones para:

- Mantener una conversación en español.
- Consultar información real mediante las herramientas MCP.
- Evitar inventar productos, precios, cantidades o existencias.
- Presentar información clara sobre los productos disponibles.
- Mostrar el contenido y valor total del carrito.
- Consultar las direcciones de entrega disponibles.
- Presentar un resumen del pedido y solicitar autorización explícita antes de confirmarlo.

La interfaz ADK Web permite interactuar directamente con el agente y observar las herramientas utilizadas durante la conversación.

## 8. Funcionamiento general

El sistema permite desarrollar el proceso de compra mediante las siguientes etapas:

1. **Consulta del catálogo:** el usuario solicita información sobre las categorías y productos disponibles.
2. **Selección de productos:** el agente consulta los productos y verifica sus existencias mediante las herramientas MCP.
3. **Administración del carrito:** el usuario puede agregar productos, modificar cantidades, eliminar elementos y consultar el total de su compra.
4. **Preparación del pedido:** el chatbot presenta un resumen de los productos seleccionados y permite consultar las direcciones de entrega.
5. **Confirmación:** después de recibir la autorización explícita del usuario, se solicita al servidor la confirmación del pedido.
6. **Actualización de la información:** el servidor verifica las condiciones del pedido, descuenta las unidades correspondientes del inventario y actualiza su estado a confirmado.

Este proceso permite integrar la interacción mediante lenguaje natural con operaciones reales sobre una base de datos relacional.

## 9. Seguridad y consideraciones

Durante la implementación se tuvieron en cuenta algunos aspectos relacionados con la seguridad y la integridad de la información:

- Las credenciales de MySQL y la clave de acceso a Gemini se gestionan mediante variables de entorno.
- El servidor valida los identificadores, cantidades y existencias de los productos.
- Se verifica que la dirección seleccionada pertenezca al cliente correspondiente.
- La confirmación del pedido utiliza transacciones y bloqueos para proteger la actualización del inventario.
- El agente solicita autorización explícita antes de ejecutar la confirmación de una compra.

La implementación actual está orientada a un entorno local de desarrollo. El servidor MCP no cuenta con autenticación independiente, el sistema utiliza un único cliente de demostración y no contempla funcionalidades de pago ni cancelación de pedidos. Además, la autorización previa a la confirmación depende de las instrucciones del agente y no de una validación independiente del servidor.

## 10. Estructura del proyecto

El repositorio está organizado en dos aplicaciones independientes:

```text
Laboratorio-MCP-Supermercado/
│
├── labmcp/
│   ├── src/main/java/
│   │   ├── model/
│   │   ├── repository/
│   │   ├── service/
│   │   └── dto/
│   ├── src/main/resources/
│   ├── src/test/
│   └── pom.xml
│
├── agentemcp/
│   ├── src/main/java/
│   ├── src/main/resources/
│   ├── src/test/
│   └── pom.xml
│
└── README.md
```

**labmcp:** contiene el servidor MCP, las entidades, los repositorios, los servicios de negocio y las pruebas correspondientes.

**agentemcp:** contiene el agente de inteligencia artificial, su configuración de conexión con MCP y la interfaz conversacional mediante ADK Web.

## 11. Ejecución del proyecto

Para ejecutar la aplicación se requiere contar con Java 21, Git Bash, MySQL 8, una base de datos compatible con el modelo del proyecto y una clave válida de Gemini.

El funcionamiento requiere iniciar primero el servidor MCP y posteriormente el agente conversacional, debido a que este último descubre las herramientas disponibles durante su inicialización.

Los servicios se ejecutan localmente en las siguientes direcciones:

- **Servidor MCP:** `http://localhost:8088/mcp`
- **Interfaz del chatbot:** `http://localhost:8080`

Las credenciales se configuran mediante las variables de entorno `PWD_UDEA` y `GOOGLE_API_KEY`. La dirección del servidor MCP puede ajustarse mediante `MCP_SERVER_URL`.

## 12. Autores

**Universidad de Antioquia**  
Facultad de Ingeniería  
Ingeniería de Sistemas  
Asignatura: Arquitectura de Software  
Docente: Diego José Luis Botia V.

Integrantes:
- Daniel Mesa Patiño
- Valeria Isabel Vásquez Barragán