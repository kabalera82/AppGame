package k82studio.models;

import java.util.ArrayList;
import java.util.List;

public class Inventario {

    public List<Personaje> personajes = new ArrayList<>();

    public void agregar(Personaje p) { personajes.add(p);};
    public void eliminar(Personaje p) {personajes.remove(p);};
    public List<Personaje> getTodos() { return personajes;};
    public Personaje getPorIndice(int i){ return personajes.get(i);}
    public int getTamanio(){ return personajes.size();};
}
