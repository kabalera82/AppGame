package k82studio.models;

public class Mago extends Personaje{

    private int magia;

    public Mago (String nombre, int nivel, int magia) {
        super(nombre, 70, magia);
        this.magia = magia;
    }
    @Override
    public int atacar() {
        return magia * 4;
    }
    @Override
    public String getTipoAtaque() {
        return "Bola de Fuego";
    }

    @Override
    public String getResumen() {
        return "[MAGO] " + getNombre()
                + " | Vida: "   + getVida()
                + " | Magia: "  + magia
                + " | Nivel: "  + getNivel();
    }
}
