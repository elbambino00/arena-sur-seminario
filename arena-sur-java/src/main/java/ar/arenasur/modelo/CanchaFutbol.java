package ar.arenasur.modelo;
public final class CanchaFutbol extends Cancha {
    public CanchaFutbol(long id, String nombre) { super(id, nombre); }
    @Override public String getDeporte() { return "Fútbol 5"; }
}
