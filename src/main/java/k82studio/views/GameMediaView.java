package k82studio.views;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * Vista de demostración de recursos multimedia en JavaFX.
 * Muestra cómo cargar imágenes, efectos de sonido y música de fondo.
 *
 * Recursos esperados en src/main/resources/:
 *   images/guerrero.png  — sprite del personaje
 *   audio/ataque.mp3     — sonido de ataque
 *   audio/musica.mp3     — música de fondo (opcional)
 */
public class GameMediaView extends StackPane {

    private MediaPlayer musicaFondo;
    private AudioClip   sonidoAtaque;

    public GameMediaView() {
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #0d0d1a;");

        cargarImagen();
        cargarAudio();
    }

    private void cargarImagen() {
        try {
            Image img = new Image(
                getClass().getResourceAsStream("/images/guerrero.png")
            );
            ImageView vista = new ImageView(img);
            vista.setFitWidth(400);
            vista.setPreserveRatio(true);
            getChildren().add(vista);
        } catch (Exception e) {
            // nunca dejes que un recurso que falta crashee la app
            System.err.println("Imagen no encontrada: " + e.getMessage());
        }
    }

    private void cargarAudio() {
        try {
            sonidoAtaque = new AudioClip(
                getClass().getResource("/audio/ataque.mp3").toExternalForm()
            );
        } catch (Exception e) {
            System.err.println("Audio de ataque no encontrado: " + e.getMessage());
        }
    }

    public void reproducirAtaque() {
        if (sonidoAtaque != null) sonidoAtaque.play();
    }

    public void iniciarMusica(String ruta) {
        try {
            Media media = new Media(getClass().getResource(ruta).toExternalForm());
            musicaFondo = new MediaPlayer(media);
            musicaFondo.setCycleCount(MediaPlayer.INDEFINITE);
            musicaFondo.setVolume(0.3);
            musicaFondo.play();
        } catch (Exception e) {
            System.err.println("Música no encontrada: " + e.getMessage());
        }
    }

    public void detenerMusica() {
        if (musicaFondo != null) musicaFondo.stop();
    }
}
