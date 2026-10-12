package ar.arenasur.modelo;
public final class CanchaPadel extends Cancha {
    public CanchaPadel(long id, String nombre) { super(id, nombre); }
    @Override public String getDeporte() { return "Pádel"; }
}
