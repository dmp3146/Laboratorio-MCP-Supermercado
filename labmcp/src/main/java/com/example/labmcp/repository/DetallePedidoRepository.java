package com.example.labmcp.repository;

import com.example.labmcp.model.DetallePedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {

    @EntityGraph(attributePaths = "producto")
    List<DetallePedido> findAllByPedido_IdOrderByIdAsc(Long pedidoId);

    @EntityGraph(attributePaths = "producto")
    Optional<DetallePedido> findByPedido_IdAndProducto_Id(Long pedidoId, Long productoId);
}
