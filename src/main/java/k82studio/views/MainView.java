package k82studio.views;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import k82studio.controllers.PersonajeController;
import k82studio.models.Inventario;
import k82studio.models.Personaje;

import java.util.List;

public class MainView extends BorderPane {

    private StackPane centro;
    private PersonajeController controller;

    public MainView(Inventario inventario) {
        this.controller = new PersonajeController(inventario, this);
        setLeft(crearMenu(inventario));
        setCenter(crearCentro());
    }

    private VBox crearMenu(Inventario inventario) {
        VBox menu = new VBox(8);
        menu.setPadding(new Insets(15));
        menu.setPrefWidth(220);
        menu.setStyle("-fx-background-color: #2c3e50;");

        Label titulo = new Label("PERSONAJES");
        titulo.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        menu.getChildren().add(titulo);

        // un botón por cada personaje — automático
        for (int i = 0; i < inventario.getTamanio(); i++) {
            final int indice = i;
            Button btn = new Button(inventario.getPorIndice(i).getNombre());
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white;");
            btn.setOnAction(e -> controller.seleccionarPersonaje(indice));
            menu.getChildren().add(btn);
        }

        Button btnJugar = new Button("▶ JUGAR");
        btnJugar.setMaxWidth(Double.MAX_VALUE);
        btnJugar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnJugar.setOnAction(e -> abrirJuego(inventario));
        menu.getChildren().add(btnJugar);

        return menu;
    }

    private StackPane crearCentro() {
        centro = new StackPane();
        centro.setStyle("-fx-background-color: #ecf0f1;");
        Label placeholder = new Label("Selecciona un personaje");
        placeholder.setStyle("-fx-font-size: 18px; -fx-text-fill: #95a5a6;");
        centro.getChildren().add(placeholder);
        return centro;
    }

    // métodos públicos que llama el Controller
    public void mostrarPersonaje(Personaje p) {
        centro.getChildren().clear();
        centro.getChildren().add(new PersonajeView(p));
    }

    public void mostrarLista(List<Personaje> lista) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        for (Personaje p : lista) {
            box.getChildren().add(new Label(p.getResumen()));
        }
        centro.getChildren().clear();
        centro.getChildren().add(box);
    }

    private void abrirJuego(Inventario inventario) {
        GameView gameView = new GameView(inventario.getPorIndice(0));
        centro.getChildren().clear();
        centro.getChildren().add(gameView);
        gameView.requestFocus(); // importante para capturar teclas
    }
}