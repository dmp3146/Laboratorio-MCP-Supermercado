package com.example.labmcp.repository;

import com.example.labmcp.model.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DireccionRepository extends JpaRepository<Direccion, Long> {

    List<Direccion> findByCliente_IdOrderByPrincipalDescIdAsc(Long clienteId);

    Optional<Direccion> findByIdAndCliente_Id(Long id, Long clienteId);
}
