package k82studio.views;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import k82studio.models.Personaje;

import java.util.HashSet;
import java.util.Set;

public class GameView extends VBox {

    private double x = 400, y = 300;
    private final double VELOCIDAD = 3;
    private int vidaActual;
    private final int vidaMax;

    // guarda TODAS las teclas pulsadas a la vez → permite diagonal
    private final Set<KeyCode> teclasActivas = new HashSet<>();

    private final Canvas canvas;
    private final ProgressBar barraVida;
    private final Label labelVida;
    private final Personaje personaje;

    public GameView(Personaje personaje) {
        this.personaje  = personaje;
        this.vidaMax    = personaje.getVida();
        this.vidaActual = vidaMax;

        setPadding(new Insets(10));
        setSpacing(8);
        setStyle("-fx-background-color: #1a1a2e;");

        labelVida = new Label("Vida: " + vidaActual + " / " + vidaMax);
        labelVida.setStyle("-fx-text-fill: white;");

        barraVida = new ProgressBar(1.0);
        barraVida.setPrefWidth(400);
        barraVida.setStyle("-fx-accent: #2ecc71;");

        canvas = new Canvas(900, 550);

        getChildren().addAll(labelVida, barraVida, canvas);

        setFocusTraversable(true);
        setOnKeyPressed(e  -> teclasActivas.add(e.getCode()));
        setOnKeyReleased(e -> teclasActivas.remove(e.getCode()));

        iniciarBucle();
    }

    private void iniciarBucle() {
        new AnimationTimer() {
            @Override public void handle(long now) {
                mover();
                dibujar();
            }
        }.start();
    }

    private void mover() {
        double w = canvas.getWidth(), h = canvas.getHeight();
        if (teclasActivas.contains(KeyCode.LEFT)  && x > 20)    x -= VELOCIDAD;
        if (teclasActivas.contains(KeyCode.RIGHT) && x < w - 20) x += VELOCIDAD;
        if (teclasActivas.contains(KeyCode.UP)    && y > 20)    y -= VELOCIDAD;
        if (teclasActivas.contains(KeyCode.DOWN)  && y < h - 20) y += VELOCIDAD;

        if (teclasActivas.contains(KeyCode.Q)) cambiarVida(-1);
        if (teclasActivas.contains(KeyCode.E)) cambiarVida(+1);
    }

    private void dibujar() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#16213e"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        String color = personaje.getTipoAtaque().equals("Espada") ? "#3498db" : "#9b59b6";
        gc.setFill(Color.web(color));
        gc.fillOval(x - 20, y - 20, 40, 40);

        gc.setFill(Color.WHITE);
        gc.fillText(personaje.getNombre(), x - 25, y - 28);
    }

    private void cambiarVida(int cantidad) {
        vidaActual = Math.max(0, Math.min(vidaMax, vidaActual + cantidad));
        double pct = (double) vidaActual / vidaMax;
        barraVida.setProgress(pct);
        labelVida.setText("Vida: " + vidaActual + " / " + vidaMax);

        if (pct > 0.5)       barraVida.setStyle("-fx-accent: #2ecc71;");
        else if (pct > 0.25) barraVida.setStyle("-fx-accent: #f39c12;");
        else                 barraVida.setStyle("-fx-accent: #e74c3c;");
    }
}