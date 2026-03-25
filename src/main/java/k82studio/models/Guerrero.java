package k82studio.models;

public class Guerrero extends Personaje {

    private int fuerza;

    public Guerrero(String nombre, int nivel, int fuerza) {
        super(nombre, 120, nivel); // más vida que el mago
        this.fuerza = fuerza;
    }

    @Override
    public int atacar() {
        return fuerza * 2;
    }
    @Override
    public String getTipoAtaque() {
        return "Estocada";
    }

    @Override
    public String getResumen() {
        return "[GUERRERO] " + getNombre()
                + " | Vida: "   + getVida()
                + " | Fuerza: " + fuerza
                + " | Nivel: "  + getNivel();
    }
}
