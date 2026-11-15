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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "direccion", uniqueConstraints = {
        @UniqueConstraint(name = "uk_direccion_id_cliente", columnNames = {"id", "cliente_id"})
})
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "direccion", nullable = false, length = 250)
    private String direccion;

    @Column(name = "barrio", length = 100)
    private String barrio;

    @Column(name = "ciudad", nullable = false, length = 100)
    private String ciudad;

    @Column(name = "codigo_postal", length = 20)
    private String codigoPostal;

    @Column(name = "indicaciones", length = 250)
    private String indicaciones;

    @Column(name = "principal")
    private Boolean principal;

    protected Direccion() {
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getBarrio() {
        return barrio;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public String getIndicaciones() {
        return indicaciones;
    }

    public Boolean getPrincipal() {
        return principal;
    }
}
