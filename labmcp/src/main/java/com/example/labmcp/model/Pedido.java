package com.example.labmcp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedido")
public class Pedido {

    public static final String ESTADO_CARRITO = "carrito";
    public static final String ESTADO_CONFIRMADO = "confirmado";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direccion_id", referencedColumnName = "id")
    private Direccion direccion;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    @Column(name = "total", precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "estado", length = 30)
    private String estado;

    protected Pedido() {
    }

    public Pedido(Cliente cliente, LocalDateTime fecha, BigDecimal total, String estado) {
        this.cliente = cliente;
        this.fecha = fecha;
        this.total = total;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Direccion getDireccion() {
        return direccion;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getEstado() {
        return estado;
    }

    public void setDireccion(Direccion direccion) {
        this.direccion = direccion;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
