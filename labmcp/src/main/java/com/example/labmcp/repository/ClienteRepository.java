package com.example.labmcp.repository;

import com.example.labmcp.model.Cliente;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cliente c where c.id = :id")
    Optional<Cliente> findLockedById(@Param("id") Long id);
}
