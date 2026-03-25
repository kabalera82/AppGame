package k82studio.models;

public class Arquero extends Personaje {

    private int disparo;

    public Arquero (String nombre, int nivel, int disparo){
        super(nombre, 100, disparo);
        this.disparo = disparo;
    }
    @Override
    public int atacar(){
        return disparo*3;
    }
    @Override
    public String getTipoAtaque() {
        return "Flecha Rapida";
    }

    @Override
    public String getResumen() {
        return "[MAGO] " + getNombre()
                + " | Vida: "   + getVida()
                + " | Disparo: "  + disparo
                + " | Nivel: "  + getNivel();
    }

}
