package com.example.labmcp.service;

import com.example.labmcp.config.DemoClientProperties;
import com.example.labmcp.dto.SupermercadoDtos.ClienteDto;
import com.example.labmcp.dto.SupermercadoDtos.DireccionDto;
import com.example.labmcp.model.Cliente;
import com.example.labmcp.repository.ClienteRepository;
import com.example.labmcp.repository.DireccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final DemoClientProperties demoClientProperties;
    private final ClienteRepository clienteRepository;
    private final DireccionRepository direccionRepository;

    public ClienteService(DemoClientProperties demoClientProperties,
                          ClienteRepository clienteRepository,
                          DireccionRepository direccionRepository) {
        this.demoClientProperties = demoClientProperties;
        this.clienteRepository = clienteRepository;
        this.direccionRepository = direccionRepository;
    }

    @Transactional(readOnly = true)
    public ClienteDto consultarClienteDemo() {
        return SupermercadoMapper.cliente(obtenerClienteDemo());
    }

    @Transactional(readOnly = true)
    public List<DireccionDto> listarDirecciones() {
        return direccionRepository
                .findByCliente_IdOrderByPrincipalDescIdAsc(demoClientProperties.clientId())
                .stream()
                .map(SupermercadoMapper::direccion)
                .toList();
    }

    Cliente obtenerClienteDemo() {
        return clienteRepository.findById(demoClientProperties.clientId())
                .orElseThrow(() -> new BusinessException("CLIENTE_DEMO_NO_ENCONTRADO",
                        "No se encontró el cliente de demostración configurado."));
    }
}
