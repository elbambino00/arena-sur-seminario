package ar.arenasur.modelo;
public final class CanchaTenis extends Cancha {
    public CanchaTenis(long id, String nombre) { super(id, nombre); }
    @Override public String getDeporte() { return "Tenis"; }
}
