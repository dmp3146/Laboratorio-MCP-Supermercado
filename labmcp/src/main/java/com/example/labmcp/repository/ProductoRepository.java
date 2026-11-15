package com.example.labmcp.repository;

import com.example.labmcp.model.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @EntityGraph(attributePaths = "categoria")
    @Query("""
            select p from Producto p
            where p.activo = true
              and (:texto is null or lower(p.nombre) like lower(concat('%', :texto, '%')))
              and (:categoriaId is null or p.categoria.id = :categoriaId)
            order by p.nombre
            """)
    List<Producto> buscarActivos(@Param("texto") String texto,
                                 @Param("categoriaId") Long categoriaId,
                                 Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    Optional<Producto> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findLockedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id in :ids order by p.id")
    List<Producto> findAllLockedByIdInOrderById(@Param("ids") Collection<Long> ids);
}
