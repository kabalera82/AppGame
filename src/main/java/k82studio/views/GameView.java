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
import k82studio.models.Enemigo;
import k82studio.models.Personaje;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class GameView extends VBox {

    // ── CONSTANTES ────────────────────────────────────────────
    private static final double RADIO_JUGADOR  = 20.0;
    private static final double RADIO_ENEMIGO  = 18.0;
    private static final double DIST_COMBATE   = 50.0;
    private static final double VELOCIDAD_PX_SEG = 200.0; // píxeles por segundo

    // ── ESTADO ────────────────────────────────────────────────
    private double x = 400, y = 300;
    private long ultimoFrame = 0;
    private int vidaActual;
    private final int vidaMax;
    private boolean combateActivo = false; // evita disparar combate múltiples veces

    // guarda TODAS las teclas pulsadas a la vez → permite diagonal
    private final Set<KeyCode> teclasActivas = new HashSet<>();
    private final List<Enemigo> enemigos = new ArrayList<>();

    // ── UI ────────────────────────────────────────────────────
    private final Canvas canvas;
    private final ProgressBar barraVida;
    private final Label labelVida;
    private final Personaje personaje;
    private AnimationTimer timer;

    // Callback que MainView registra para saber cuándo hay combate
    private Consumer<Enemigo> onCombateListener;

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

        // Al perder el foco, limpiar teclas activas para evitar movimiento eterno
        focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) teclasActivas.clear();
        });

        // Pedir foco automáticamente al añadirse a la escena
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) requestFocus();
        });

        // Recuperar foco al hacer clic sobre el canvas
        setOnMouseClicked(e -> requestFocus());

        inicializarEnemigos();
        iniciarBucle();
    }

    // ── INICIALIZAR ENEMIGOS ──────────────────────────────────
    private void inicializarEnemigos() {
        enemigos.add(new Enemigo("Orco",   60, 15, 80,  700, 100));
        enemigos.add(new Enemigo("Troll", 100, 25, 50,  200, 500));
        enemigos.add(new Enemigo("Goblin", 40, 10, 120, 100, 300));
    }

    private void iniciarBucle() {
        timer = new AnimationTimer() {
            @Override public void handle(long ahora) {
                if (ultimoFrame == 0) {
                    ultimoFrame = ahora;
                    return; // primer frame: solo inicializar
                }

                // delta = tiempo transcurrido desde el último frame en segundos
                double delta = (ahora - ultimoFrame) / 1_000_000_000.0;
                ultimoFrame = ahora;

                mover(delta);
                actualizarEnemigos(delta);
                comprobarColisiones();
                dibujar();
            }
        };
        timer.start();
    }

    // ── MOVIMIENTO con delta time ─────────────────────────────
    private void mover(double delta) {
        double dist = VELOCIDAD_PX_SEG * delta; // píxeles a mover este frame
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // movimiento con límites correctos (el radio evita que se salga)
        if (teclasActivas.contains(KeyCode.LEFT)  && x - RADIO_JUGADOR > 0)    x -= dist;
        if (teclasActivas.contains(KeyCode.RIGHT) && x + RADIO_JUGADOR < w)    x += dist;
        if (teclasActivas.contains(KeyCode.UP)    && y - RADIO_JUGADOR > 0)    y -= dist;
        if (teclasActivas.contains(KeyCode.DOWN)  && y + RADIO_JUGADOR < h)    y += dist;
    }

    // ── ACTUALIZAR ENEMIGOS cada frame ────────────────────────
    private void actualizarEnemigos(double delta) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        for (Enemigo e : enemigos) {
            if (e.isVivo()) {
                e.actualizar(delta, x, y, w, h);
            }
        }
    }

    // ── COMPROBAR COLISIONES ──────────────────────────────────
    private void comprobarColisiones() {
        if (combateActivo) return; // ya hay un combate activo

        for (Enemigo e : enemigos) {
            if (!e.isVivo()) continue;

            double dx = x - e.getX();
            double dy = y - e.getY();
            double distancia = Math.sqrt(dx * dx + dy * dy);

            if (distancia < DIST_COMBATE) {
                iniciarCombate(e);
                break; // solo un combate a la vez
            }
        }
    }

    // ── INICIAR COMBATE ───────────────────────────────────────
    private void iniciarCombate(Enemigo enemigo) {
        combateActivo = true;
        timer.stop(); // detiene el AnimationTimer del mapa

        if (onCombateListener != null) {
            onCombateListener.accept(enemigo);
        }
    }

    // Llamado cuando el combate termina para reanudar el mapa
    public void reanudar() {
        combateActivo = false;
        ultimoFrame = 0;
        timer.start();
        requestFocus();
    }

    public void setOnCombate(Consumer<Enemigo> listener) {
        this.onCombateListener = listener;
    }

    // ── DIBUJAR ───────────────────────────────────────────────
    private void dibujar() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#16213e"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // dibujar enemigos
        dibujarEnemigos(gc);

        // dibujar jugador
        String color = personaje.getTipoAtaque().equals("Estocada") ? "#3498db" : "#9b59b6";
        gc.setFill(Color.web(color));
        gc.fillOval(x - RADIO_JUGADOR, y - RADIO_JUGADOR, RADIO_JUGADOR * 2, RADIO_JUGADOR * 2);

        gc.setFill(Color.WHITE);
        gc.fillText(personaje.getNombre(), x - 25, y - 28);
    }

    // ── DIBUJAR ENEMIGOS ─────────────────────────────────────
    private void dibujarEnemigos(GraphicsContext gc) {
        for (Enemigo e : enemigos) {
            if (!e.isVivo()) continue;

            // cuerpo del enemigo (rojo)
            gc.setFill(Color.web("#e74c3c"));
            gc.fillOval(e.getX() - RADIO_ENEMIGO, e.getY() - RADIO_ENEMIGO,
                        RADIO_ENEMIGO * 2, RADIO_ENEMIGO * 2);

            // nombre encima
            gc.setFill(Color.WHITE);
            gc.fillText(e.getNombre(), e.getX() - 20, e.getY() - 25);

            // mini barra de vida del enemigo
            double pct = (double) e.getVida() / e.getVidaMax();
            gc.setFill(Color.web("#2ecc71"));
            gc.fillRect(e.getX() - 20, e.getY() - 22, 40 * pct, 4);
            gc.setStroke(Color.WHITE);
            gc.strokeRect(e.getX() - 20, e.getY() - 22, 40, 4);
        }
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
