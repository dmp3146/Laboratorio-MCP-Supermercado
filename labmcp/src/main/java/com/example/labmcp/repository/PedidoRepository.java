package com.example.labmcp.repository;

import com.example.labmcp.model.Pedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Pedido p
            where p.cliente.id = :clienteId and p.estado = 'carrito'
            order by p.id
            """)
    List<Pedido> findCartsLockedByClienteId(@Param("clienteId") Long clienteId);

    @Query("""
            select p from Pedido p
            where p.cliente.id = :clienteId and p.estado = 'carrito'
            order by p.id
            """)
    List<Pedido> findCartsByClienteId(@Param("clienteId") Long clienteId);

    @EntityGraph(attributePaths = "direccion")
    @Query("""
            select p from Pedido p
            where p.id = :pedidoId and p.cliente.id = :clienteId
            """)
    Optional<Pedido> findByIdAndClienteId(@Param("pedidoId") Long pedidoId,
                                          @Param("clienteId") Long clienteId);

    @EntityGraph(attributePaths = "direccion")
    List<Pedido> findByCliente_IdAndEstadoOrderByFechaDescIdDesc(Long clienteId, String estado);
}
