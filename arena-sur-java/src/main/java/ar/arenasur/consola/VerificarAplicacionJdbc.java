package ar.arenasur.consola;


/** Punto de entrada mínimo para comprobar que la aplicación ve datos persistidos. */
public final class VerificarAplicacionJdbc {
    private VerificarAplicacionJdbc() {
    }

    public static void main(String[] args) {
        ContextoAplicacionJdbc contexto = ContextoAplicacionJdbc.desdeEntorno();
        var deportes = contexto.deportes().listarActivos();
        var canchas = contexto.canchas().listarActivas();
        var tarifas = contexto.tarifas().listarActivas();
        var clientes = contexto.clientes().listarActivos();
        var reservas = contexto.reservas().listar();
        System.out.println("ARENA SUR JDBC");
        System.out.println("Deportes activos: " + deportes.size());
        System.out.println("Canchas activas: " + canchas.size());
        System.out.println("Tarifas activas: " + tarifas.size());
        System.out.println("Clientes activos: " + clientes.size());
        System.out.println("Reservas persistidas: " + reservas.size());
    }
}
