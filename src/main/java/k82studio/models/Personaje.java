package k82studio.models;

import k82studio.interfaces.Atacable;
import k82studio.interfaces.Mostrable;

public abstract class Personaje implements Atacable, Mostrable {
    private String nombre;
    private int vida;
    private int nivel;

    public Personaje(String nombre, int vida, int nivel) {
        this.nombre = nombre;
        this.vida   = vida;
        this.nivel = nivel;
    }

    public String getNombre() { return nombre; }
    public int getVida() { return vida; }
    public int getNivel() { return nivel; }
    public void setVida(int v){
        this.vida = v;
    }

    @Override
    public abstract int atacar();
    @Override
    public abstract String getTipoAtaque();
    @Override
    public abstract String getResumen();

}
