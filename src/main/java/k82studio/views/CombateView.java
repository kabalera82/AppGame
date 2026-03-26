package k82studio.views;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import k82studio.controllers.CombateController;
import k82studio.models.Enemigo;
import k82studio.models.Personaje;

public class CombateView extends VBox {

    private Label infoJugador, infoEnemigo, logLabel;
    private ProgressBar vidaJugador, vidaEnemigo;
    private Button btnAtacar, btnDefender, btnHuir;
    private StackPane zonaJugador, zonaEnemigo;
    private CombateController controller;
    private Runnable onCombateTerminado;
    private final int vidaMaxJugador;

    public CombateView(Personaje jugador, Enemigo enemigo) {
        this.vidaMaxJugador = jugador.getVida();

        setSpacing(20);
        setPadding(new Insets(30));
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #0d0d1a;");

        this.controller = new CombateController(jugador, enemigo, this);

        // ── ZONA ENEMIGO ──────────────────────────────────────
        zonaEnemigo = crearZonaPersonaje("#e74c3c", "ENEMIGO");
        infoEnemigo = new Label(enemigo.getNombre() + " | Vida: " + enemigo.getVida());
        infoEnemigo.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 16px;");
        vidaEnemigo = crearBarraVida();
        vidaEnemigo.setStyle("-fx-accent: #e74c3c;");

        // ── ZONA JUGADOR ──────────────────────────────────────
        zonaJugador = crearZonaPersonaje("#3498db", "JUGADOR");
        infoJugador = new Label(jugador.getResumen());
        infoJugador.setStyle("-fx-text-fill: #3498db; -fx-font-size: 16px;");
        vidaJugador = crearBarraVida();
        vidaJugador.setStyle("-fx-accent: #3498db;");

        // ── LOG ────────────────────────────────────────────────
        logLabel = new Label("¡Combate iniciado!");
        logLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 14px; -fx-font-style: italic;");

        // ── BOTONES ────────────────────────────────────────────
        btnAtacar   = new Button("⚔  Atacar");
        btnDefender = new Button("🛡  Defender");
        btnHuir     = new Button("🏃  Huir");
        HBox botones = new HBox(15, btnAtacar, btnDefender, btnHuir);
        botones.setAlignment(Pos.CENTER);

        btnAtacar.setOnAction(e   -> controller.atacar());
        btnDefender.setOnAction(e -> controller.defender());
        btnHuir.setOnAction(e     -> controller.huir());

        getChildren().addAll(
            zonaEnemigo, infoEnemigo, vidaEnemigo,
            logLabel,
            vidaJugador, infoJugador, zonaJugador,
            botones
        );
    }

    // ── MÉTODOS QUE LLAMA EL CONTROLLER ───────────────────────

    public void log(String mensaje) {
        logLabel.setText(mensaje);
    }

    public void actualizar(Personaje j, Enemigo e) {
        infoJugador.setText(j.getResumen());
        infoEnemigo.setText(e.getNombre() + " | Vida: " + e.getVida());
        vidaJugador.setProgress((double) j.getVida() / vidaMaxJugador);
        vidaEnemigo.setProgress((double) e.getVida() / e.getVidaMax());
    }

    // Efecto flash cuando recibe daño
    public void mostrarEfectoAtaque(String quien) {
        StackPane zona = quien.equals("jugador") ? zonaJugador : zonaEnemigo;

        FadeTransition flash = new FadeTransition(Duration.millis(100), zona);
        flash.setFromValue(1.0);
        flash.setToValue(0.2);
        flash.setCycleCount(4);      // parpadea 4 veces
        flash.setAutoReverse(true);
        flash.play();
    }

    public void deshabilitarBotones(boolean deshabilitar) {
        btnAtacar.setDisable(deshabilitar);
        btnDefender.setDisable(deshabilitar);
        btnHuir.setDisable(deshabilitar);
    }

    public void mostrarVictoria() {
        log("¡¡VICTORIA!! Has derrotado al enemigo!");
        deshabilitarBotones(true);
        if (onCombateTerminado != null) {
            // espera 2 segundos y vuelve al mapa
            new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.seconds(2),
                    e -> onCombateTerminado.run())
            ).play();
        }
    }

    public void mostrarDerrota() {
        log("Has sido derrotado... Game Over");
        deshabilitarBotones(true);
    }

    public void cerrar() {
        if (onCombateTerminado != null) onCombateTerminado.run();
    }

    public void setOnCombateTerminado(Runnable callback) {
        this.onCombateTerminado = callback;
    }

    // ── HELPERS ────────────────────────────────────────────────
    private StackPane crearZonaPersonaje(String color, String etiqueta) {
        StackPane zona = new StackPane();
        zona.setPrefSize(120, 120);

        Rectangle fondo = new Rectangle(120, 120);
        fondo.setFill(Color.web(color + "33")); // color con 20% opacidad
        fondo.setArcWidth(10);
        fondo.setArcHeight(10);

        Label label = new Label(etiqueta);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px; -fx-font-weight: bold;");

        zona.getChildren().addAll(fondo, label);
        return zona;
    }

    private ProgressBar crearBarraVida() {
        ProgressBar barra = new ProgressBar(1.0);
        barra.setPrefWidth(300);
        barra.setStyle("-fx-accent: #2ecc71;");
        return barra;
    }
}
