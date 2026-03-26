package k82studio.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import k82studio.models.Enemigo;
import k82studio.models.Personaje;
import k82studio.views.CombateView;

public class CombateController {

    private final Personaje jugador;
    private final Enemigo   enemigo;
    private final CombateView vista;
    private boolean turnoJugador = true;
    private boolean enAnimacion  = false; // evita clicks durante animación
    private boolean defendiendo  = false; // flag para reducir daño del siguiente ataque

    public CombateController(Personaje jugador, Enemigo enemigo, CombateView vista) {
        this.jugador = jugador;
        this.enemigo = enemigo;
        this.vista   = vista;
    }

    // ── ATAQUE con animación ───────────────────────────────────
    public void atacar() {
        if (enAnimacion) return; // bloquear botones durante animación
        enAnimacion = true;
        vista.deshabilitarBotones(true);

        Timeline timeline = new Timeline(

            // 0ms: mostrar animación de ataque del jugador
            new KeyFrame(Duration.millis(0), e -> {
                vista.mostrarEfectoAtaque("jugador");
                vista.log(jugador.getNombre() + " prepara su ataque...");
            }),

            // 400ms: aplicar daño al enemigo
            new KeyFrame(Duration.millis(400), e -> {
                int danio = jugador.atacar();
                enemigo.recibirDanio(danio);
                vista.log(jugador.getNombre() + " golpea por " + danio + " de daño!");
                vista.actualizar(jugador, enemigo);
                vista.mostrarEfectoAtaque("enemigo");
            }),

            // 800ms: comprobar si el enemigo murió
            new KeyFrame(Duration.millis(800), e -> {
                if (!enemigo.isVivo()) {
                    vista.mostrarVictoria();
                    enAnimacion = false;
                    return;
                }
                // si sigue vivo, el enemigo contraataca
                turnoEnemigo();
            })
        );
        timeline.play();
    }

    // ── TURNO DEL ENEMIGO ──────────────────────────────────────
    private void turnoEnemigo() {
        Timeline timeline = new Timeline(

            new KeyFrame(Duration.millis(0), e ->
                vista.log(enemigo.getNombre() + " prepara su ataque...")
            ),

            new KeyFrame(Duration.millis(500), e -> {
                int danio = enemigo.getAtaque();

                // si el jugador se está defendiendo, reducir daño a la mitad
                if (defendiendo) {
                    danio = danio / 2;
                    defendiendo = false;
                    vista.log(enemigo.getNombre() + " ataca, pero bloqueas! Solo " + danio + " de daño.");
                } else {
                    vista.log(enemigo.getNombre() + " golpea por " + danio + "!");
                }

                int nuevaVida = Math.max(0, jugador.getVida() - danio);
                jugador.setVida(nuevaVida);
                vista.actualizar(jugador, enemigo);
                vista.mostrarEfectoAtaque("jugador");
            }),

            new KeyFrame(Duration.millis(900), e -> {
                if (jugador.getVida() <= 0) {
                    vista.mostrarDerrota();
                } else {
                    vista.deshabilitarBotones(false); // devolver control al jugador
                }
                enAnimacion = false;
            })
        );
        timeline.play();
    }

    // ── DEFENSA ────────────────────────────────────────────────
    // Activa el flag defendiendo → reduce daño del próximo ataque enemigo
    public void defender() {
        if (enAnimacion) return;
        defendiendo = true;
        vista.log(jugador.getNombre() + " se prepara para defender el próximo ataque.");
    }

    public void huir() {
        if (enAnimacion) return;
        vista.log("Has huido del combate.");
        vista.cerrar();
    }
}
