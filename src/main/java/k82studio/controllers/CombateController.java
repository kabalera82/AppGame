package k82studio.controllers;

import k82studio.models.Personaje;
import k82studio.views.CombateView;

public class CombateController {

    private Personaje jugador;
    private Personaje enemigo;
    private CombateView vista;
    private boolean turnoJugador = true;

    public CombateController(Personaje jugador, Personaje enemigo, CombateView vista) {
        this.jugador = jugador;
        this.enemigo = enemigo;
        this.vista   = vista;
    }

    public void atacar() {
        if (turnoJugador) {
            int danio = jugador.atacar();
            enemigo.setVida(enemigo.getVida() - danio);
            vista.log(jugador.getNombre() + " ataca por " + danio);
        } else {
            int danio = enemigo.atacar();
            jugador.setVida(jugador.getVida() - danio);
            vista.log(enemigo.getNombre() + " contraataca por " + danio);
        }
        turnoJugador = !turnoJugador;
        vista.actualizar(jugador, enemigo);
        comprobarFin();
    }

    public void defender() {
        vista.log(jugador.getNombre() + " se defiende — daño reducido");
        turnoJugador = !turnoJugador;
        vista.actualizar(jugador, enemigo);
    }

    public void huir() {
        vista.log("Has huido del combate");
        vista.cerrar();
    }

    private void comprobarFin() {
        if (enemigo.getVida() <= 0) vista.mostrarVictoria();
        if (jugador.getVida() <= 0) vista.mostrarDerrota();
    }
}