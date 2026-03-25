package k82studio.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import k82studio.models.Personaje;

public class PersonajeView extends VBox {

    public PersonajeView(Personaje personaje) {
        setSpacing(10);
        setPadding(new Insets(30));
        setAlignment(Pos.CENTER);

        Label nombre  = new Label("Nombre: " + personaje.getNombre());
        Label vida    = new Label("Vida: "   + personaje.getVida());
        Label ataque  = new Label("Ataque: " + personaje.getTipoAtaque());

        nombre.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        vida.setStyle("-fx-font-size: 18px;");
        ataque.setStyle("-fx-font-size: 18px;");

        getChildren().addAll(nombre, vida, ataque);
    }
}