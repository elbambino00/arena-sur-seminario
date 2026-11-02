package ar.arenasur.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Configuración JDBC inmutable obtenida fuera del código fuente. */
public record ConfiguracionBaseDatos(String url, String usuario, String password) {
    public static final String VARIABLE_URL = "ARENA_SUR_DB_URL";
    public static final String VARIABLE_USUARIO = "ARENA_SUR_DB_USER";
    public static final String VARIABLE_PASSWORD = "ARENA_SUR_DB_PASSWORD";

    public ConfiguracionBaseDatos {
        url = validar(url, VARIABLE_URL).trim();
        usuario = validar(usuario, VARIABLE_USUARIO).trim();
        password = validar(password, VARIABLE_PASSWORD);
    }

    public static ConfiguracionBaseDatos desdeEntorno() {
        return desde(System.getenv());
    }

    public static ConfiguracionBaseDatos desde(Map<String, String> variables) {
        List<String> faltantes = new ArrayList<>();
        agregarSiFalta(variables, VARIABLE_URL, faltantes);
        agregarSiFalta(variables, VARIABLE_USUARIO, faltantes);
        agregarSiFalta(variables, VARIABLE_PASSWORD, faltantes);

        if (!faltantes.isEmpty()) {
            throw new IllegalArgumentException(
                "Faltan variables de entorno para MySQL: " + String.join(", ", faltantes));
        }

        return new ConfiguracionBaseDatos(
            variables.get(VARIABLE_URL),
            variables.get(VARIABLE_USUARIO),
            variables.get(VARIABLE_PASSWORD));
    }

    private static void agregarSiFalta(Map<String, String> variables, String nombre, List<String> faltantes) {
        if (variables.get(nombre) == null || variables.get(nombre).isBlank()) {
            faltantes.add(nombre);
        }
    }

    private static String validar(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(nombre + " no puede estar vacío");
        }
        return valor;
    }
}
