package k82studio.controllers;

import k82studio.models.Inventario;
import k82studio.models.Personaje;
import k82studio.views.MainView;

public class PersonajeController {

    private Inventario inventario;
    private MainView vista;

    public PersonajeController(Inventario inventario, MainView vista) {
        this.inventario = inventario;
        this.vista      = vista;
    }

    public void seleccionarPersonaje(int indice) {
        Personaje p = inventario.getPorIndice(indice);
        vista.mostrarPersonaje(p);
    }

    public void mostrarTodos() {
        vista.mostrarLista(inventario.getTodos());
    }
}
