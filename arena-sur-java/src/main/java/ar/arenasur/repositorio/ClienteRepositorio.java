package ar.arenasur.repositorio;

import ar.arenasur.modelo.Cliente;
import ar.arenasur.modelo.DatosCliente;
import java.util.List;
import java.util.Optional;

public interface ClienteRepositorio {
    Cliente guardar(DatosCliente datos);
    List<Cliente> listarActivos();
    Optional<Cliente> buscarActivo(long id);
    Optional<Cliente> buscarActivoPorEmail(String email);
}
